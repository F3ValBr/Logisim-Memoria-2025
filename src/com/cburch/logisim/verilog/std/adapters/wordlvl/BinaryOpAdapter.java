package com.cburch.logisim.verilog.std.adapters.wordlvl;

// BinaryOpAdapter.java

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.FactoryLookup;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.BinaryOp;
import com.cburch.logisim.verilog.std.ComponentAdapter;
import com.cburch.logisim.verilog.std.InstanceHandle;
import com.cburch.logisim.verilog.std.PinLocator;
import com.cburch.logisim.verilog.std.Strings;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;

import java.awt.Graphics;

public final class BinaryOpAdapter implements ComponentAdapter {

    private final ModuleBlackBoxAdapter fallback = new ModuleBlackBoxAdapter();

    @Override
    public boolean accepts(CellType t) {
        return t != null && t.isWordLevel() && t.isBinary();
    }

    @Override
    public InstanceHandle create(Canvas canvas, Graphics g, VerilogCell cell, Location where) {
        final BinaryOp op;
        try {
            op = BinaryOp.fromYosys(cell.type().typeId());
        } catch (Exception e) {
            return fallback.create(canvas, g, cell, where);
        }

        // 1) Elegir factory según operación ($and/$or/$xor/$xnor → Gates; $add/$sub/$mul → Arithmetic)
        ComponentFactory factory = pickFactoryOrNull(canvas.getProject(), op);
        if (factory == null) {
            // no soportado nativamente → subcircuito
            return fallback.create(canvas, g, cell, where);
        }

        // 2) Width heurístico (Yosys: A_WIDTH/B_WIDTH/Y_WIDTH)
        int width = guessBinaryWidth(cell.params());

        try {
            Project proj = canvas.getProject();
            Circuit circ = canvas.getCircuit();

            AttributeSet attrs = factory.createAttributeSet();

            // Ancho de bus
            try {
                attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
            } catch (Exception ignore) { /* algunos factories no exponen WIDTH */ }

            // Etiqueta
            try {
                attrs.setValue(StdAttr.LABEL, cell.name());
            } catch (Exception ignore) { }

            // Nota: Para $add/$sub Logisim “Adder/Subtractor” tienen pines Cin/Cout.
            // Aquí solo creamos el componente; el cableado (p. ej. Cin=0) lo resolverás en tu fase de wiring/túneles.

            Component comp = factory.createComponent(where, attrs);

            if (circ.hasConflict(comp)) throw new CircuitException(Strings.get("exclusiveError"));
            Bounds bds = comp.getBounds(g);
            if (bds.getX() < 0 || bds.getY() < 0) throw new CircuitException(Strings.get("negativeCoordError"));

            CircuitMutation m = new CircuitMutation(circ);
            m.add(comp);
            proj.doAction(m.toAction(Strings.getter("addComponentAction", factory.getDisplayGetter())));

            PinLocator pins = (port, bit) -> comp.getLocation(); // placeholder; mapea luego por puerto si lo necesitas
            return new InstanceHandle(comp, pins);

        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir " + op + ": " + e.getMessage(), e);
        }
    }

    /** Selecciona el ComponentFactory nativo de Logisim según la operación. */
    private static ComponentFactory pickFactoryOrNull(Project proj, BinaryOp op) {
        // Gates clásicos
        if (op == BinaryOp.AND || op == BinaryOp.OR || op == BinaryOp.XOR || op == BinaryOp.XNOR) {
            Library gates = proj.getLogisimFile().getLibrary("Gates");
            if (gates == null) return null;
            String gateName = switch (op) {
                case AND  -> "AND Gate";
                case OR   -> "OR Gate";
                case XOR  -> "XOR Gate";
                case XNOR -> "XNOR Gate";
                default   -> null;
            };
            return FactoryLookup.findFactory(gates, gateName);
        }

        // Aritméticos (suma/resta/mult)
        if (op == BinaryOp.ADD || op == BinaryOp.SUB || op == BinaryOp.MUL || op == BinaryOp.GE|| op == BinaryOp. GT|| op == BinaryOp. EQ|| op == BinaryOp. NE|| op == BinaryOp. LE|| op == BinaryOp. LT) {
            Library arith = proj.getLogisimFile().getLibrary("Arithmetic");
            if (arith == null) return null;
            String name = switch (op) {
                case ADD -> "Adder";
                case SUB -> "Subtractor";
                case MUL -> "Multiplier";
                case GE, GT, EQ, NE, LE, LT -> "Comparator";
                default  -> null;
            };
            return FactoryLookup.findFactory(arith, name);
        }

        // Otros binarios (comparadores, shifts, lógica-AND/OR, etc.) → no mapeados aquí
        return null;
    }

    /** Heurística para WIDTH en binarios Yosys. */
    private static int guessBinaryWidth(CellParams params) {
        if (params instanceof GenericCellParams g) {
            Object aw = g.asMap().get("A_WIDTH");
            Object bw = g.asMap().get("B_WIDTH");
            Object yw = g.asMap().get("Y_WIDTH");
            int a = parseIntRelaxed(aw, 1);
            int b = parseIntRelaxed(bw, a);
            int y = parseIntRelaxed(yw, Math.max(a, b));
            return Math.max(1, Math.max(Math.max(a, b), y));
        }
        return 1;
    }

    private static int parseIntRelaxed(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return def;
        if (s.matches("[01xXzZ]+")) {
            s = s.replaceAll("[xXzZ]", "0");
            try { return Integer.parseInt(s, 2); } catch (Exception ignore) { return def; }
        }
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}

