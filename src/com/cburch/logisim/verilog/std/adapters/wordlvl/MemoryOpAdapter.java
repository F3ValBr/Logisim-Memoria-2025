package com.cburch.logisim.verilog.std.adapters.wordlvl;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.FactoryLookup;
import com.cburch.logisim.verilog.comp.auxiliary.LogicalMemory;
import com.cburch.logisim.verilog.comp.auxiliary.SupportsFactoryLookup;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleImpl;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOp;
import com.cburch.logisim.verilog.layout.MemoryIndex;
import com.cburch.logisim.verilog.std.AbstractComponentAdapter;
import com.cburch.logisim.verilog.std.InstanceHandle;
import com.cburch.logisim.verilog.std.PinLocator;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;

import java.awt.*;

public final class MemoryOpAdapter extends AbstractComponentAdapter
        implements SupportsFactoryLookup {

    private final ModuleBlackBoxAdapter fallback = new ModuleBlackBoxAdapter();

    // Contexto del módulo actual (inyectado por el importador)
    private MemoryIndex currentMemIndex;
    private VerilogModuleImpl currentModule;

    /** Llamar desde el importador justo después de construir el MemoryIndex del módulo. */
    public void beginModule(MemoryIndex idx, VerilogModuleImpl mod) {
        this.currentMemIndex = idx;
        this.currentModule   = mod;
    }

    @Override
    public boolean accepts(CellType t) {
        if (t == null) return false;
        // acepta todo lo marcado como memoria y/o con los typeIds típicos
        if (t.isMemory()) return true;
        String id = t.typeId();
        return MemoryOp.isMemoryTypeId(id);
    }

    @Override
    public InstanceHandle create(Canvas canvas, Graphics g, VerilogCell cell, Location where) {
        try {
            // Si no tenemos índice del módulo, no podemos unificar → fallback
            if (currentMemIndex == null) {
                return fallback.create(canvas, g, cell, where);
            }

            // Lee MEMID de la celda
            String memId = readMemId(cell.params());
            if (memId == null || memId.isEmpty()) {
                return fallback.create(canvas, g, cell, where);
            }

            LogicalMemory lm = currentMemIndex.get(memId);
            if (lm == null) {
                return fallback.create(canvas, g, cell, where);
            }

            // Unificación: tanto $mem/$mem_v2 como $memrd/$memwr/$meminit
            // se convierten en **una** RAM/ROM. (Si ya la colocaste, puedes
            // elegir ignorar siguientes celdas del mismo MEMID; aquí lo simple:
            // crear siempre una instancia —si tu importador llama create una vez
            // por celda, verás varias RAM/ROM iguales; si quieres evitarlo,
            // habilita una marca en lm/meta o en el circuito).
            return createUnifiedRamOrRom(canvas, g, cell, where, lm);

        } catch (CircuitException e) {
            throw new IllegalStateException("MemoryOpAdapter: " + e.getMessage(), e);
        }
    }

    @Override
    public ComponentFactory peekFactory(Project proj, VerilogCell cell) {
        // Si logramos determinar RAM vs ROM aquí, devolvemos esa factory
        String memId = readMemId(cell.params());
        if (memId == null || currentMemIndex == null) return null;

        LogicalMemory lm = currentMemIndex.get(memId);
        if (lm == null) return null;

        boolean hasWrite = !lm.writePortIdxs().isEmpty();
        Library lib = pickMemoryLibrary(proj);
        if (lib == null) return null;

        String compName = hasWrite ? "RAM" : "ROM";
        return FactoryLookup.findFactory(lib, compName);
    }

    /* =======================================================================
       Implementación
       ======================================================================= */

    private InstanceHandle createUnifiedRamOrRom(Canvas canvas, Graphics g,
                                                 VerilogCell anyCellOfThisMem,
                                                 Location where,
                                                 LogicalMemory lm)
            throws CircuitException {
        Project proj = canvas.getProject();
        Circuit circ = canvas.getCircuit();

        // 1) Deducir parámetros (width / depth / abits)
        int width  = 8;
        int depth  = 256;
        int abits  = 8;

        // Prioriza meta (si vino del JSON/YosysMemoryDTO)
        if (lm.meta() != null) {
            if (lm.meta().width() > 0) width = lm.meta().width();
            if (lm.meta().size()  > 0) depth = lm.meta().size();
        }

        // Si la celda tiene ABITS/WIDTH definidos, (re)ajusta
        if (anyCellOfThisMem.params() instanceof GenericCellParams gp) {
            int w = parseIntRelaxed(gp.asMap().get("WIDTH"), -1);
            int a = parseIntRelaxed(gp.asMap().get("ABITS"), -1);
            if (w > 0) width = w;
            if (a > 0) abits = a;
        }

        // Si de meta vino sólo width/size, calcula abits desde depth (o viceversa)
        if (abits <= 0 && depth > 0) {
            abits = Math.max(1, ceilLog2(depth));
        }
        if (depth <= 0 && abits > 0) {
            depth = 1 << abits;
        }

        // 2) Elegir RAM vs ROM (si hay writePorts → RAM, si no → ROM)
        boolean hasWrite = !lm.writePortIdxs().isEmpty();

        // 3) Elegir librería/Factory
        Library memLib = pickMemoryLibrary(proj);
        if (memLib == null) {
            return fallback.create(canvas, g, anyCellOfThisMem, where);
        }

        String compName = hasWrite ? "RAM" : "ROM";
        ComponentFactory f = FactoryLookup.findFactory(memLib, compName);
        if (f == null) {
            return fallback.create(canvas, g, anyCellOfThisMem, where);
        }

        // 4) Atributos para tu RAM/ROM
        AttributeSet attrs = f.createAttributeSet();

        // OJO: estos nombres dependen de tus componentes RAM/ROM reales
        // Ajusta si tus atributos tienen otros "name tokens":
        //   - Ejemplos comunes: "width", "addrBits", "depth" o "dataWidth" / "addressBits"
        setParsedByName(attrs, "dataWidth", Integer.toString(width));   // TODO: ajusta nombre si difiere
        setParsedByName(attrs, "addrWidth", Integer.toString(abits));   // TODO: ajusta nombre si difiere
        setParsedByName(attrs, "depth",    Integer.toString(depth));   // TODO: si tu RAM lo expone
        setParsedIfPresent(attrs, "label", cleanCellName(lm.memId())); // etiqueta amistosa

        Component comp = addComponent(proj, circ, g, f, where, attrs);

        // PinLocator trivial (si luego quieres mapear puertos a pins físicos)
        PinLocator pins = (port, bit) -> comp.getLocation();
        return new InstanceHandle(comp, pins);
    }

    private static Library pickMemoryLibrary(Project proj) {
        if (proj == null || proj.getLogisimFile() == null) return null;
        // 1º intenta la librería nativa de Logisim
        Library mem = proj.getLogisimFile().getLibrary("Memory");
        if (mem != null) return mem;
        // 2º intenta tu librería custom, si es donde viven RAM/ROM
        return proj.getLogisimFile().getLibrary("Yosys Components");
    }

    private static String readMemId(CellParams p) {
        if (p instanceof GenericCellParams g) {
            Object v = g.asMap().get("MEMID");
            if (v == null) return null;
            String s = String.valueOf(v).trim();
            // Yosys suele anteponer "\" a los IDs RTLIL
            if (s.startsWith("\\")) s = s.substring(1);
            return s;
        }
        return null;
    }

    private static int ceilLog2(int n) {
        if (n <= 1) return 1;
        int k = 32 - Integer.numberOfLeadingZeros(n - 1);
        return Math.max(k, 1);
    }
}
