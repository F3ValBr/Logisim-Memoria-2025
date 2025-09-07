package com.cburch.logisim.verilog.std.adapters.wordlvl;

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
import com.cburch.logisim.verilog.comp.specs.wordlvl.UnaryOp;
import com.cburch.logisim.verilog.std.ComponentAdapter;
import com.cburch.logisim.verilog.std.InstanceHandle;
import com.cburch.logisim.verilog.std.PinLocator;
import com.cburch.logisim.verilog.std.Strings;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;

import java.awt.Graphics;

/**
 * Adapter para operaciones unarias word-level.
 * Crea NOT/BUF nativos de Logisim con ancho de bus.
 * Para otras unarias delega al módulo (caja negra).
 */
public final class UnaryOpAdapter implements ComponentAdapter {

    private final ModuleBlackBoxAdapter fallback = new ModuleBlackBoxAdapter();

    @Override
    public boolean accepts(CellType t) {
        // Solo word-level & kind UNARY
        return t != null && t.isWordLevel() && t.isUnary();
    }

    @Override
    public InstanceHandle create(Canvas canvas, Graphics g, VerilogCell cell, Location where) {
        UnaryOp op = UnaryOp.fromYosys(cell.type().typeId());
        try {
            Project proj = canvas.getProject();
            Circuit circ = canvas.getCircuit();

            // Intentar mapear a gates nativos: $not, $buf
            ComponentFactory gateFactory = pickFactoryOrNull(op, proj);
            if (gateFactory == null) {
                // Otros ($neg, $pos, $logic_not, $reduce_*) → fallback a subcircuito
                return fallback.create(canvas, g, cell, where);
            }

            // Extraer WIDTH de parámetros Yosys (A_WIDTH o Y_WIDTH)
            int width = guessUnaryWidth(cell.params());

            AttributeSet attrs = gateFactory.createAttributeSet();
            // Establecer bit width (si el gate lo soporta)
            try {
                attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
            } catch (Exception ignore) {
                // Algunos gates usan StdAttr.WIDTH (según tu árbol). Intenta StdAttr si aplica:
                try {
                    attrs.setValue(com.cburch.logisim.instance.StdAttr.WIDTH, BitWidth.create(width));
                } catch (Exception ignoredToo) { /* si no aplica, lo ignoramos */ }
            }

            // Etiqueta con el nombre de la celda (visual)
            try {
                attrs.setValue(com.cburch.logisim.instance.StdAttr.LABEL, cell.name());
            } catch (Exception ignore) { }

            Component comp = gateFactory.createComponent(where, attrs);

            // Validaciones básicas
            if (circ.hasConflict(comp)) throw new CircuitException(Strings.get("exclusiveError"));
            Bounds bds = comp.getBounds(g);
            if (bds.getX() < 0 || bds.getY() < 0) throw new CircuitException(Strings.get("negativeCoordError"));

            CircuitMutation m = new CircuitMutation(circ);
            m.add(comp);
            proj.doAction(m.toAction(Strings.getter("addComponentAction", gateFactory.getDisplayGetter())));

            // Localizador trivial de pines (si luego quieres mapear por puerto, cámbialo)
            PinLocator pins = (port, bit) -> comp.getLocation();
            return new InstanceHandle(comp, pins);

        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir " + op + ": " + e.getMessage(), e);
        }
    }

    /** Mapea solo BUF/NOT a factories nativas; resto null (fallback) */
    private static ComponentFactory pickFactoryOrNull(UnaryOp op, Project proj) {
        Library gatesLib = proj.getLogisimFile().getLibrary("Gates");
        if (gatesLib == null) return null;

        return switch (op) {
            case BUF -> FactoryLookup.findFactory(gatesLib, "Buffer");
            case NOT -> FactoryLookup.findFactory(gatesLib, "NOT Gate");
            default  -> null; // otros unarios todavía no tienen equivalente
        };
    }

    /** Heurística de ancho para unarias Yosys. */
    private static int guessUnaryWidth(CellParams params) {
        if (params instanceof GenericCellParams g) {
            // Yosys suele poner A_WIDTH / Y_WIDTH como enteros o strings bin/dec
            Object aw = g.asMap().get("A_WIDTH");
            Object yw = g.asMap().get("Y_WIDTH");
            int a = parseIntRelaxed(aw, 1);
            int y = parseIntRelaxed(yw, a > 0 ? a : 1);
            // Para $not/$buf, A_WIDTH == Y_WIDTH normalmente
            return Math.max(1, Math.max(a, y));
        }
        return 1;
    }

    /** Parser tolerante (número o string decimal/binario). */
    private static int parseIntRelaxed(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return def;
        // Yosys a veces da binarios con 32 bits "000...01010"
        if (s.matches("[01xXzZ]+")) {
            // trata 'x','z' como 0
            s = s.replaceAll("[xXzZ]", "0");
            try { return Integer.parseInt(s, 2); } catch (Exception ignore) { return def; }
        }
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
