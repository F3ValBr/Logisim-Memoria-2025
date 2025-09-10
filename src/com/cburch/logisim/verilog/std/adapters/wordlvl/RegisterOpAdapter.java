package com.cburch.logisim.verilog.std.adapters.wordlvl;

// RegisterOpAdapter.java

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.*;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.FactoryLookup;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.std.*;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;

import java.awt.Graphics;

public final class RegisterOpAdapter extends AbstractComponentAdapter {

    private final ModuleBlackBoxAdapter fallback = new ModuleBlackBoxAdapter();

    @Override
    public boolean accepts(CellType t) {
        return t != null && t.isWordLevel() && t.isRegister();
    }

    @Override
    public InstanceHandle create(Canvas canvas, Graphics g, VerilogCell cell, Location where) {
        // 1) Buscar “Register” en Memory
        ComponentFactory factory = pickRegisterFactory(canvas.getProject());
        if (factory == null) {
            // Si no existe, usa fallback a subcircuito (caja negra)
            return fallback.create(canvas, g, cell, where);
        }

        // 2) WIDTH desde parámetros (default 1)
        int width = guessWidth(cell.params());

        try {
            Project proj = canvas.getProject();
            Circuit circ = canvas.getCircuit();

            AttributeSet attrs = factory.createAttributeSet();

            // Ancho de datos
            try { attrs.setValue(StdAttr.WIDTH, BitWidth.create(Math.max(1, width))); } catch (Exception ignore) {}

            // Etiqueta
            try { attrs.setValue(StdAttr.LABEL, cell.name()); } catch (Exception ignore) {}

            // Borde de reloj desde CLK_POLARITY (si el Register expone un atributo “Trigger”/“Edge”)
            setClockEdgeIfPossible(attrs, cell.params());

            Component comp = addComponent(proj, circ, g, factory, where, attrs);
            // PinLocator placeholder; más adelante podrás ubicar pines reales
            PinLocator pins = (port, bit) -> comp.getLocation();
            return new InstanceHandle(comp, pins);

        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir Register: " + e.getMessage(), e);
        }
    }

    /* ===== helpers ===== */

    private static ComponentFactory pickRegisterFactory(Project proj) {
        Library mem = proj.getLogisimFile().getLibrary("Memory");
        // En la mayoría de builds el displayName es “Register”
        ComponentFactory f = FactoryLookup.findFactory(mem, "Register");
        if (f == null) {
            // Algunas traducciones pueden llamarlo distinto; añade alias si lo necesitas.
            f = FactoryLookup.findFactory(mem, "Registro"); // ejemplo
        }
        return f;
    }

    private static int guessWidth(CellParams p) {
        if (p instanceof GenericCellParams g) {
            Object w = g.asMap().get("WIDTH");
            return parseIntRelaxed(w, 1);
        }
        return 1;
    }

    /** Intenta fijar el borde de reloj según CLK_POLARITY: 1→rising, 0→falling. */
    @SuppressWarnings("unchecked")
    private static void setClockEdgeIfPossible(AttributeSet attrs, CellParams params) {
        if (!(params instanceof GenericCellParams g)) return;
        Object cp = g.asMap().get("CLK_POLARITY");
        Integer pol = (cp == null) ? null : Integer.valueOf(parseIntRelaxed(cp, 1)); // default 1 → rising

        if (pol == null) return;

        // Busca un atributo de tipo elección cuyo displayName o name sugiera “Trigger”/“Edge”
        Attribute<?> edgeAttr = null;
        for (Attribute<?> a : attrs.getAttributes()) {
            String dn = a.getDisplayName();
            String nm = a.getName();
            Class<?> vc = a.getClass();
            if (vc != null && Enum.class.isAssignableFrom(vc)) {
                boolean looksLikeEdge =
                        (dn != null && (dn.toLowerCase().contains("trigger") || dn.toLowerCase().contains("edge")))
                                || (nm != null && (nm.toLowerCase().contains("trigger") || nm.toLowerCase().contains("edge")));
                if (looksLikeEdge) { edgeAttr = a; break; }
            }
        }
        if (edgeAttr == null) return;

        // Intenta elegir uno de los enum values que encaje con rising/falling
        try {
            Class<?> ec = edgeAttr.getClass();
            Object choice = null;
            for (Object ev : ec.getEnumConstants()) {
                String s = ev.toString().toLowerCase();
                if (pol != 0 && (s.contains("rise") || s.contains("rising") || s.contains("pos"))) { choice = ev; break; }
                if (pol == 0 && (s.contains("fall") || s.contains("falling") || s.contains("neg"))) { choice = ev; break; }
            }
            if (choice != null) {
                @SuppressWarnings("rawtypes")
                Attribute cast = (Attribute) edgeAttr;
                attrs.setValue(cast, choice);
            }
        } catch (Exception ignore) { }
    }
}
