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
import com.cburch.logisim.verilog.std.adapters.MacroRegistry;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;
import com.cburch.logisim.verilog.std.macrocomponents.ComposeCtx;
import com.cburch.logisim.verilog.std.macrocomponents.Factories;
import com.cburch.logisim.verilog.std.macrocomponents.Macro;


import java.awt.Graphics;

/**
 * Adapter para operaciones unarias word-level.
 * Crea NOT/BUF/NEG/POS nativos de Logisim con ancho de bus.
 * Para otras unarias genera una composicion de componentes
 * o delega al módulo (caja negra).
 */
public final class UnaryOpAdapter extends AbstractComponentAdapter
                                    implements SupportsFactoryLookup {

    private final ModuleBlackBoxAdapter fallback = new ModuleBlackBoxAdapter();
    private final MacroRegistry registry = MacroRegistry.bootUnaryDefaults();

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

            // 1) Intenta receta registrada (composición)
            MacroRegistry.Recipe recipe = registry.find(cell.type().typeId());
            if (recipe != null) {
                var ctx = new ComposeCtx(proj, circ, g,
                        Factories.warmup(proj));
                Macro m = recipe.build(ctx, cell, where);
                // PinLocator usando el pinMap, si quieres enrutar por nombre:
                PinLocator pins = (port, bit) -> m.pinMap.getOrDefault(port, m.root.getLocation());
                return new InstanceHandle(m.root, pins);
            }

            // 2) Si no hay receta, se procede con componentes nativos
            ComponentFactory factory = pickFactoryOrNull(proj, op);
            if (factory == null) return fallback.create(canvas, g, cell, where);

            int width = guessUnaryWidth(cell.params());
            AttributeSet attrs = factory.createAttributeSet();
            try { attrs.setValue(StdAttr.WIDTH, BitWidth.create(width)); } catch (Exception ignore) {}
            try { attrs.setValue(StdAttr.LABEL, cleanCellName(cell.name())); } catch (Exception ignore) {}

            Component comp = addComponent(proj, circ, g, factory, where, attrs);
            return new InstanceHandle(comp, (port, bit) -> comp.getLocation());

        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir " + op + ": " + e.getMessage(), e);
        }
    }

    /** Intenta obtener factory nativo para sizing previo a creación (si aplica). */
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
                    case POS -> "Buffer";
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
    public static int guessUnaryWidth(CellParams params) {
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
