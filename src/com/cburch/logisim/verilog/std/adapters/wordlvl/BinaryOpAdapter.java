package com.cburch.logisim.verilog.std.adapters.wordlvl;

// BinaryOpAdapter.java

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
import com.cburch.logisim.verilog.comp.specs.wordlvl.BinaryOp;
import com.cburch.logisim.verilog.std.*;
import com.cburch.logisim.verilog.std.adapters.MacroRegistry;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;
import com.cburch.logisim.verilog.std.macrocomponents.ComposeCtx;
import com.cburch.logisim.verilog.std.macrocomponents.Factories;

import java.awt.Graphics;

public final class BinaryOpAdapter extends AbstractComponentAdapter
                                    implements SupportsFactoryLookup {

    private final ModuleBlackBoxAdapter fallback = new ModuleBlackBoxAdapter();
    private final MacroRegistry registry = MacroRegistry.bootBinaryDefaults();

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

        MacroRegistry.Recipe recipe = registry.find(cell.type().typeId());
        if (recipe != null) {
            var ctx = new ComposeCtx(canvas.getProject(), canvas.getCircuit(), g, Factories.warmup(canvas.getProject()));
            try {
                return recipe.build(ctx, cell, where);
            } catch (CircuitException e) {
                throw new IllegalStateException("No se pudo componer " + op + ": " + e.getMessage(), e);
            }
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
                attrs.setValue(StdAttr.LABEL, cleanCellName(cell.name()));
            } catch (Exception ignore) { }

            // Nota: Para $add/$sub Logisim “Adder/Subtractor” tienen pines Cin/Cout.
            // Aquí solo creamos el componente; el cableado (p. ej. Cin=0) lo resolverás en tu fase de wiring/túneles.

            Component comp = addComponent(proj, circ, g, factory, where, attrs);

            PinLocator pins = (port, bit) -> comp.getLocation(); // placeholder; mapea luego por puerto si lo necesitas
            return new InstanceHandle(comp, pins);

        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir " + op + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ComponentFactory peekFactory(Project proj, VerilogCell cell) {
        BinaryOp op = BinaryOp.fromYosys(cell.type().typeId());
        return pickFactoryOrNull(proj, op);
    }

    /** Selecciona el ComponentFactory nativo de Logisim según la operación. */
    private static ComponentFactory pickFactoryOrNull(Project proj, BinaryOp op) {
        // Gates clásicos
        switch (op.category()) {
            case BITWISE -> {
                Library gates = proj.getLogisimFile().getLibrary("Gates");
                if (gates == null) return null;
                String gateName = switch (op) {
                    case AND -> "AND Gate";
                    case OR -> "OR Gate";
                    case XOR -> "XOR Gate";
                    case XNOR -> "XNOR Gate";
                    default -> null;
                };
                return FactoryLookup.findFactory(gates, gateName);
            }
            case ARITH -> {
                // Aritméticos (suma/resta/mult)
                Library arith = proj.getLogisimFile().getLibrary("Arithmetic");
                if (arith == null) return null;
                String name = switch (op) {
                    case ADD -> "Adder";
                    case SUB -> "Subtractor";
                    case MUL -> "Multiplier";
                    case DIV, MOD, DIVFLOOR, MODFLOOR -> "Divider";
                    case POW -> "Exponent";
                    default -> null;
                };
                return FactoryLookup.findFactory(arith, name);
            }
            case COMPARE -> {
                // Comparadores → usar Comparator (con pin de salida 'eq', 'lt', 'gt')
                Library arith = proj.getLogisimFile().getLibrary("Arithmetic");
                if (arith == null) return null;
                return FactoryLookup.findFactory(arith, "Comparator");
            }
            case SHIFT -> {
                // Shifts → usar Shift (con pin de salida 'out')
                Library arith = proj.getLogisimFile().getLibrary("Arithmetic");
                if (arith == null) return null;
                return FactoryLookup.findFactory(arith, "Shifter");
            }
            default -> {
                // Otros binarios (comparadores, shifts, lógica-AND/OR, etc.) → no mapeados aquí
                return null;
            }
        }
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
}

