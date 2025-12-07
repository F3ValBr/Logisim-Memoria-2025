package com.cburch.logisim.verilog.std.adapters.wordlvl;

// MuxOpAdapter.java

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.gates.Gates;
import com.cburch.logisim.std.plexers.Plexers;
import com.cburch.logisim.std.yosys.YosysComponent;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.FactoryLookup;
import com.cburch.logisim.verilog.comp.auxiliary.SupportsFactoryLookup;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOp;
import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;
import com.cburch.logisim.verilog.std.*;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;

import java.awt.Graphics;
import java.util.Map;

public final class MuxOpAdapter extends AbstractComponentAdapter
        implements SupportsFactoryLookup {

    private final ModuleBlackBoxAdapter fallback = new ModuleBlackBoxAdapter();

    @Override
    public boolean accepts(CellType t) {
        return t != null && t.isWordLevel() && t.isMultiplexer();
    }

    @Override
    public InstanceHandle create(Project proj, Circuit circ, Graphics g, VerilogCell cell, Location where) {
        final MuxOp op;
        try {
            op = MuxOp.fromYosys(cell.type().typeId());
        } catch (Exception e) {
            // tipo desconocido → fallback
            return fallback.create(proj, circ, g, cell, where);
        }

        LibFactory lf = pickFactoryOrNull(proj, op);
        if (lf == null || lf.factory() == null) {
            // No hay mapeo nativo → subcircuito
            return fallback.create(proj, circ, g, cell, where);
        }

        // Heurísticas de ancho (datos) y select (si aplica)
        int dataWidth = guessWidth(cell.params());     // WIDTH de datos (si existe)
        int selWidth  = guessSelectWidth(cell.params()); // S_WIDTH si el op lo tiene (bmux/pmux, etc.)

        try {
            AttributeSet attrs = lf.factory().createAttributeSet();

            // Intentar fijar ancho de bus (cuando el factory expose StdAttr.WIDTH)
            try {
                attrs.setValue(StdAttr.WIDTH, BitWidth.create(dataWidth));
            } catch (Exception ignore) { }

            // Intentar fijar select bits si el factory lo soporta (Multiplexer/Binary Multiplexer, etc.)
            try {
                if (selWidth > 0) {
                    attrs.setValue(Plexers.ATTR_SELECT, BitWidth.create(selWidth));
                }
            } catch (Exception ignore) { }
            setIntByName(attrs,"swidth", selWidth);

            // Etiqueta visible
            try {
                attrs.setValue(StdAttr.LABEL, cleanCellName(cell.name()));
            } catch (Exception ignore) { }

            // Deshabilitar enable si lo hubiera (algunos plexers lo exponen)
            try {
                attrs.setValue(Plexers.ATTR_ENABLE, Boolean.FALSE);
            } catch (Exception ignore) { }

            Component comp = addComponent(proj, circ, g, lf.factory(), where, attrs);

            // Mapa nombre->índice específico de ESTA instancia (usa library + factory + instance)
            Map<String,Integer> nameToIdx =
                    BuiltinPortMaps.forFactory(lf.lib(), lf.factory(), comp);

            PortGeom pg = PortGeom.of(comp, nameToIdx);
            return new InstanceHandle(comp, pg);
        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir " + op + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ComponentFactory peekFactory(Project proj, VerilogCell cell) {
        MuxOp op = MuxOp.fromYosys(cell.type().typeId());
        LibFactory lf = pickFactoryOrNull(proj, op);
        return lf == null ? null : lf.factory();
    }

    /** Selecciona el ComponentFactory nativo de Logisim para cada op soportada. */
    private static LibFactory pickFactoryOrNull(Project proj, MuxOp op) {
        LogisimFile lf = proj.getLogisimFile();
        switch (op) {
            case MUX -> {
                Library plex = lf.getLibrary(Plexers.LIB_NAME);
                if (plex == null) return null;
                ComponentFactory f = FactoryLookup.findFactory(plex, Plexers.MULTIPLEXER_ID);
                return (f == null) ? null : new LibFactory(plex, f);
            }
            case DEMUX -> {
                Library plex = lf.getLibrary(Plexers.LIB_NAME);
                if (plex == null) return null;
                ComponentFactory f = FactoryLookup.findFactory(plex, Plexers.DEMULTIPLEXER_ID);
                return (f == null) ? null : new LibFactory(plex, f);
            }
            case TRIBUF -> {
                Library gates = lf.getLibrary(Gates.LIB_NAME);
                if (gates == null) return null;
                ComponentFactory f = FactoryLookup.findFactory(gates, Gates.CONTROLLED_BUFFER_ID);
                return (f == null) ? null : new LibFactory(gates, f);
            }
            case BWMUX -> {
                Library yosys = lf.getLibrary(YosysComponent.LIB_NAME);
                if (yosys == null) return null;
                ComponentFactory f = FactoryLookup.findFactory(yosys, YosysComponent.BITWISE_MUX_ID);
                return (f == null) ? null : new LibFactory(yosys, f);
            }
            case PMUX -> {
                Library yosys = lf.getLibrary(YosysComponent.LIB_NAME);
                if (yosys == null) return null;
                ComponentFactory f = FactoryLookup.findFactory(yosys, YosysComponent.PRIORITY_MUX_ID);
                return (f == null) ? null : new LibFactory(yosys, f);
            }
            case BMUX -> {
                Library yosys = lf.getLibrary(YosysComponent.LIB_NAME);
                if (yosys == null) return null;
                ComponentFactory f = FactoryLookup.findFactory(yosys, YosysComponent.BINARY_MUX_ID);
                return (f == null) ? null : new LibFactory(yosys, f);
            }
            default -> { return null; }
        }
    }

    private static int guessWidth(CellParams params) {
        if (params instanceof MuxOpParams mp) {
            int w = mp.width();
            return Math.max(1, w);
        }
        if (params instanceof GenericCellParams g) {
            Object w = g.asMap().get("WIDTH");
            int width = parseIntRelaxed(w, 1);
            return Math.max(1, width);
        }
        return 1;
    }

    /** S_WIDTH para bmux/pmux o similares; si no existe, devuelve 0 y se omite. */
    private static int guessSelectWidth(CellParams params) {
        if (params instanceof GenericCellParams g) {
            Object sw = g.asMap().get("S_WIDTH");
            int s = parseIntRelaxed(sw, 0);
            return Math.max(0, s);
        }
        return 0;
    }
}
