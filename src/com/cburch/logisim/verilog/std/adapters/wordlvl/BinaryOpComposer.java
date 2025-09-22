package com.cburch.logisim.verilog.std.adapters.wordlvl;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.std.InstanceHandle;
import com.cburch.logisim.verilog.std.adapters.BaseComposer;
import com.cburch.logisim.verilog.std.macrocomponents.ComposeCtx;
import com.cburch.logisim.verilog.std.macrocomponents.MacroSubcktKit;

import java.util.function.BiConsumer;

import static com.cburch.logisim.verilog.std.adapters.ComponentComposer.attrsWithWidthAndLabel;
import static com.cburch.logisim.verilog.std.adapters.ComponentComposer.setByNameParsed;

public final class BinaryOpComposer extends BaseComposer {
    private final MacroSubcktKit sub = new MacroSubcktKit();

    /** Y = (A != 0) AND (B != 0), con A/B/Y como Pin componentes dentro del macro */
    public InstanceHandle buildLogicAndAsSubckt(ComposeCtx ctx, VerilogCell cell, Location where,
                                                int aWidth, int bWidth) throws CircuitException {
        require(ctx.fx.cmp, "Comparator"); require(ctx.fx.notF, "NOT Gate"); require(ctx.fx.andF, "AND Gate");

        String name = MacroSubcktKit.macroName("logic_and", aWidth, bWidth);

        BiConsumer<ComposeCtx, Circuit> populate = (in, macro) -> {
            try {
                // --- 0) LAYOUT ---
                Location aLoc = Location.create(200, 100);

                // --- 1) PINS (componentes) ---
                // Inputs a la izquierda (EAST), output a la derecha (WEST)
                int ax = aLoc.getX();
                int ay = aLoc.getY();
                Location pinALoc = Location.create(ax-80, ay-30);
                Location pinBLoc = Location.create(ax-80, ay+50);
                Location pinYLoc = Location.create(ax+60, ay+20);

                Component pinA = addPin(in, "A", false, aWidth, pinALoc);
                Component pinB = addPin(in, "B", false, bWidth, pinBLoc);
                Component pinY = addPin(in, "Y", true, 1, pinYLoc);

                // --- 2) Lógica interna compacta ---
                // A == 0  →  NOT → A!=0
                Component cmpA = add(in, in.fx.cmp, aLoc,
                        attrsWithWidthAndLabel(in.fx.cmp, aWidth, "A0"));
                if (in.fx.constF != null) {
                    AttributeSet kA = in.fx.constF.createAttributeSet();
                    setByNameParsed(kA, "width", Integer.toString(aWidth));
                    setByNameParsed(kA, "value", "0x0");
                    add(in, in.fx.constF, Location.create(aLoc.getX()-40, aLoc.getY()+10), kA);
                }
                Component notA = add(in, in.fx.notF,
                        Location.create(aLoc.getX()+30, aLoc.getY()),
                        attrsWithWidthAndLabel(in.fx.notF, 1, "A!=0"));

                // B == 0  →  NOT → B!=0
                Location bLoc = Location.create(aLoc.getX(), aLoc.getY()+40);
                Component cmpB = add(in, in.fx.cmp, bLoc,
                        attrsWithWidthAndLabel(in.fx.cmp, bWidth, "B0"));
                if (in.fx.constF != null) {
                    AttributeSet kB = in.fx.constF.createAttributeSet();
                    setByNameParsed(kB, "width", Integer.toString(bWidth));
                    setByNameParsed(kB, "value", "0x0");
                    add(in, in.fx.constF, Location.create(bLoc.getX()-40, bLoc.getY()+10), kB);
                }
                Component notB = add(in, in.fx.notF,
                        Location.create(bLoc.getX()+30, bLoc.getY()),
                        attrsWithWidthAndLabel(in.fx.notF, 1, "B!=0"));

                // AND final
                Component and = add(in, in.fx.andF,
                        Location.create(aLoc.getX()+60, aLoc.getY()+20),
                        attrsWithWidthAndLabel(in.fx.andF, 1, "Y"));

                // --- 3) Wires internos ---
                // Wiring pinA -> cmpA
                addWire(in, pinA.getLocation(), pinA.getLocation().translate(10,0));
                addWire(in, pinA.getLocation().translate(10,0), cmpA.getLocation().translate(-70,-10));
                addWire(in, cmpA.getLocation().translate(-70,-10), cmpA.getLocation().translate(-40, -10));

                // Wiring pinB -> cmpB
                addWire(in, pinB.getLocation(), pinB.getLocation().translate(10,0));
                addWire(in, pinB.getLocation().translate(10,0), cmpB.getLocation().translate(-70,-10));
                addWire(in, cmpB.getLocation().translate(-70,-10), cmpB.getLocation().translate(-40, -10));

                // Wiring notA -> and
                addWire(in, notA.getLocation(), notA.getLocation().translate(0,10));
                // Wiring pinB -> and
                addWire(in, notB.getLocation(), notB.getLocation().translate(0,-10));

            } catch (CircuitException e) { throw new RuntimeException(e); }
        };

        return sub.ensureAndInstantiate(ctx, name, populate, where, lbl(cell));
    }

    /** Y = (A != 0) OR (B != 0), con A/B/Y como Pin componentes dentro del macro */
    public InstanceHandle buildLogicOrAsSubckt(ComposeCtx ctx, VerilogCell cell, Location where,
                                               int aWidth, int bWidth) throws CircuitException {
        require(ctx.fx.cmp, "Comparator"); require(ctx.fx.notF, "NOT Gate"); require(ctx.fx.orF, "OR Gate");

        String name = MacroSubcktKit.macroName("logic_or", aWidth, bWidth);

        BiConsumer<ComposeCtx, Circuit> populate = (in, macro) -> {
            try {
                // LÓGICA
                Location aLoc = Location.create(200, 100);

                // PINS
                int ax = aLoc.getX();
                int ay = aLoc.getY();
                Location pinALoc = Location.create(ax-80, ay-30);
                Location pinBLoc = Location.create(ax-80, ay+50);
                Location pinYLoc = Location.create(ax+60, ay+20);

                Component pinA = addPin(in, "A", false, aWidth, pinALoc);
                Component pinB = addPin(in, "B", false, bWidth, pinBLoc);
                Component pinY = addPin(in, "Y", true, 1, pinYLoc);

                Component cmpA = add(in, in.fx.cmp, aLoc,
                        attrsWithWidthAndLabel(in.fx.cmp, aWidth, "A0"));
                if (in.fx.constF != null) {
                    AttributeSet kA = in.fx.constF.createAttributeSet();
                    setByNameParsed(kA, "width", Integer.toString(aWidth));
                    setByNameParsed(kA, "value", "0x0");
                    add(in, in.fx.constF, Location.create(aLoc.getX()-40, aLoc.getY()+10), kA);
                }
                Component notA = add(in, in.fx.notF,
                        Location.create(aLoc.getX()+30, aLoc.getY()),
                        attrsWithWidthAndLabel(in.fx.notF, 1, "A!=0"));

                Location bLoc = Location.create(aLoc.getX(), aLoc.getY()+40);
                Component cmpB = add(in, in.fx.cmp, bLoc,
                        attrsWithWidthAndLabel(in.fx.cmp, bWidth, "B0"));
                if (in.fx.constF != null) {
                    AttributeSet kB = in.fx.constF.createAttributeSet();
                    setByNameParsed(kB, "width", Integer.toString(bWidth));
                    setByNameParsed(kB, "value", "0x0");
                    add(in, in.fx.constF, Location.create(bLoc.getX()-40, bLoc.getY()+10), kB);
                }
                Component notB = add(in, in.fx.notF,
                        Location.create(bLoc.getX()+30, bLoc.getY()),
                        attrsWithWidthAndLabel(in.fx.notF, 1, "B!=0"));

                Component or = add(in, in.fx.orF,
                        Location.create(aLoc.getX()+60, aLoc.getY()+20),
                        attrsWithWidthAndLabel(in.fx.orF, 1, "Y"));

                // WIRING
                // Wiring pinA -> cmpA
                addWire(in, pinA.getLocation(), pinA.getLocation().translate(10,0));
                addWire(in, pinA.getLocation().translate(10,0), cmpA.getLocation().translate(-70,-10));
                addWire(in, cmpA.getLocation().translate(-70,-10), cmpA.getLocation().translate(-40, -10));

                // Wiring pinB -> cmpB
                addWire(in, pinB.getLocation(), pinB.getLocation().translate(10,0));
                addWire(in, pinB.getLocation().translate(10,0), cmpB.getLocation().translate(-70,-10));
                addWire(in, cmpB.getLocation().translate(-70,-10), cmpB.getLocation().translate(-40, -10));

                // Wiring notA -> OR
                addWire(in, notA.getLocation(), notA.getLocation().translate(0,10));
                // Wiring pinB -> OR
                addWire(in, notB.getLocation(), notB.getLocation().translate(0,-10));

            } catch (CircuitException e) { throw new RuntimeException(e); }
        };

        return sub.ensureAndInstantiate(ctx, name, populate, where, lbl(cell));
    }
}
