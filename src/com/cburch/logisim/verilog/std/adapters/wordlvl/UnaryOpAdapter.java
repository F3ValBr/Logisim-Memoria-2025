package com.cburch.logisim.verilog.std.adapters.wordlvl;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.FactoryLookup;
import com.cburch.logisim.verilog.comp.auxiliary.SupportsFactoryLookup;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.UnaryOp;
import com.cburch.logisim.verilog.std.*;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;

import java.awt.Graphics;

/**
 * Adapter para operaciones unarias word-level.
 * Crea NOT/BUF nativos de Logisim con ancho de bus.
 * Para otras unarias delega al módulo (caja negra).
 */
public final class UnaryOpAdapter extends AbstractComponentAdapter
                                    implements SupportsFactoryLookup {

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

            // Caso especial: $logic_not → Splitter + NOR(inputs = width)
            if (op == UnaryOp.LOGIC_NOT) {
                return createLogicNotAsComparator(canvas, g, cell, where);
            }

            // Intentar mapear a gates nativos: $not, $buf
            ComponentFactory factory = pickFactoryOrNull(proj, op);
            if (factory == null) {
                // Otros ($neg, $pos, $logic_not, $reduce_*) → fallback a subcircuito
                return fallback.create(canvas, g, cell, where);
            }

            // Extraer WIDTH de parámetros Yosys (A_WIDTH o Y_WIDTH)
            int width = guessUnaryWidth(cell.params());

            AttributeSet attrs = factory.createAttributeSet();
            // Establecer bit width (si el gate lo soporta)
            try {
                attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
            } catch (Exception ignore) {
                // Algunos gates usan StdAttr.WIDTH (según tu árbol). Intenta StdAttr si aplica:
                try {
                    attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
                } catch (Exception ignoredToo) { /* si no aplica, lo ignoramos */ }
            }

            // Etiqueta con el nombre de la celda (visual)
            try {
                attrs.setValue(StdAttr.LABEL, cell.name());
            } catch (Exception ignore) { }

            Component comp = addComponent(proj, circ, g, factory, where, attrs);

            // Localizador trivial de pines (si luego quieres mapear por puerto, cámbialo)
            PinLocator pins = (port, bit) -> comp.getLocation();
            return new InstanceHandle(comp, pins);

        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir " + op + ": " + e.getMessage(), e);
        }
    }

    /* =========================
       $logic_not → Comparator (A ?= 0)
       ========================= */
    private InstanceHandle createLogicNotAsComparator(Canvas canvas, Graphics g, VerilogCell cell, Location where)
            throws CircuitException {

        Project proj = canvas.getProject();
        Circuit circ  = canvas.getCircuit();

        int width = Math.max(1, guessUnaryWidth(cell.params()));

        // Factories requeridas
        ComponentFactory cmpF = FactoryLookup.findFactory(proj.getLogisimFile().getLibrary("Arithmetic"), "Comparator");
        if (cmpF == null) return fallback.create(canvas, g, cell, where);

        // (Opcional) constante 0 para B
        ComponentFactory constF = FactoryLookup.findFactory(proj.getLogisimFile().getLibrary("Wiring"), "Constant");

        // Posiciones: constante a la izquierda, comparator en 'where'
        Location cmpLoc   = where;
        Location constLoc = Location.create(where.getX() - 40, where.getY() + 10);

        // Comparator: set WIDTH y etiqueta; el bit A=B será tu “salida”
        AttributeSet cmpAttrs = cmpF.createAttributeSet();
        try { cmpAttrs.setValue(StdAttr.WIDTH, BitWidth.create(width)); } catch (Exception ignore) {}
        try { cmpAttrs.setValue(StdAttr.LABEL, cell.name()); } catch (Exception ignore) {}

        Component cmpComp = addComponent(proj, circ, g, cmpF, cmpLoc, cmpAttrs);

        // Constante 0 del mismo ancho (si existe la factory). No cableamos aquí.
        if (constF != null) {
            AttributeSet kAttrs = constF.createAttributeSet();
            // estos nombres son los típicos; usamos parse-by-name para no acoplar APIs internas
            setParsedByName(kAttrs, "width", Integer.toString(width));
            setParsedByName(kAttrs, "value", "0x0");   // 0 del mismo ancho
            addComponent(proj, circ, g, constF, constLoc, kAttrs);
        }

        // Devolvemos el comparator como “componente visible” del cell
        PinLocator pins = (port, bit) -> cmpComp.getLocation();
        return new InstanceHandle(cmpComp, pins);
    }

    @Override
    public ComponentFactory peekFactory(Project proj, VerilogCell cell) {
        UnaryOp op = UnaryOp.fromYosys(cell.type().typeId());
        return pickFactoryOrNull(proj, op);
    }

    /** Mapea solo BUF/NOT a factories nativas; resto null (fallback) */
    private static ComponentFactory pickFactoryOrNull(Project proj, UnaryOp op) {
        switch (op.category()) {
            case BITWISE -> {
                Library gates = proj.getLogisimFile().getLibrary("Gates");
                if (gates == null) return null;
                String gateName = switch (op) {
                    case BUF -> "Buffer";
                    case NOT -> "NOT Gate";
                    default  -> null;
                };
                return FactoryLookup.findFactory(gates, gateName);
            }
            case ARITH -> {
                Library arith = proj.getLogisimFile().getLibrary("Arithmetic");
                if (arith == null) return null;
                String arithName = switch (op) {
                    case NEG -> "Negator";
                    default  -> null;
                };
                return FactoryLookup.findFactory(arith, arithName);
            }
            default -> {
                // Otros unarios no tienen equivalente nativo → fallback
                return null;
            }
        }
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
}
