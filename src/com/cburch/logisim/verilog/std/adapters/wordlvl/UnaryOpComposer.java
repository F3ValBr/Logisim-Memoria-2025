package com.cburch.logisim.verilog.std.adapters.wordlvl;

import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.std.adapters.BaseComposer;
import com.cburch.logisim.verilog.std.macrocomponents.ComposeCtx;
import com.cburch.logisim.verilog.std.macrocomponents.Macro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.cburch.logisim.verilog.std.adapters.ComponentComposer.*;

/** Compositor de unarias ($logic_not, $reduce_*) */
public final class UnaryOpComposer extends BaseComposer {

    /** $logic_not(A) := Comparator(A == 0), usamos salida EQ. */
    public Macro buildLogicNotEqZero(ComposeCtx ctx, VerilogCell cell, Location where, int aWidth)
            throws CircuitException {
        if (ctx.fx.cmp == null) throw new CircuitException("Comparator not found");
        List<Component> parts = new ArrayList<>();
        Map<String,Location> pins = new LinkedHashMap<>();

        AttributeSet ca = attrsWithWidthAndLabel(ctx.fx.cmp, aWidth, lbl(cell));
        Component cmp = add(ctx, ctx.fx.cmp, where, ca);
        parts.add(cmp);
        pins.put("Y", pinComparatorEQ(cmp));

        if (ctx.fx.constF != null) {
            AttributeSet ka = ctx.fx.constF.createAttributeSet();
            setByNameParsed(ka, "width", Integer.toString(aWidth));
            setByNameParsed(ka, "value", "0x0");
            parts.add(add(ctx, ctx.fx.constF, Location.create(where.getX()-40, where.getY()+10), ka));
        }
        return new Macro(cmp, parts, pins);
    }

    /** $reduce_or / $reduce_bool := NOT( A == 0 ). */
    public Macro buildReduceOrNeZero(ComposeCtx ctx, VerilogCell cell, Location where, int aWidth)
            throws CircuitException {
        if (ctx.fx.cmp == null || ctx.fx.notF == null) throw new CircuitException("Comparator/NOT not found");
        List<Component> parts = new ArrayList<>();
        Map<String,Location> pins = new LinkedHashMap<>();

        AttributeSet ca = attrsWithWidthAndLabel(ctx.fx.cmp, aWidth, lbl(cell));
        Component cmp = add(ctx, ctx.fx.cmp, where, ca);
        parts.add(cmp);

        if (ctx.fx.constF != null) {
            AttributeSet ka = ctx.fx.constF.createAttributeSet();
            setByNameParsed(ka, "width", Integer.toString(aWidth));
            setByNameParsed(ka, "value", "0x0");
            parts.add(add(ctx, ctx.fx.constF, Location.create(where.getX()-40, where.getY()+10), ka));
        }

        AttributeSet na = attrsWithWidthAndLabel(ctx.fx.notF, 1, lbl(cell) + "_ne0");
        Component not = add(ctx, ctx.fx.notF, Location.create(where.getX()+30, where.getY()), na);
        parts.add(not);

        // (opcional) cablear internamente EQ → NOT.in con Wire(...) en tu capa de wiring
        pins.put("Y", pinNotOut(not));
        return new Macro(not, parts, pins);
    }

    /** $reduce_and(A) := (A == 2^N-1). */
    public Macro buildReduceAndEqAllOnes(ComposeCtx ctx, VerilogCell cell, Location where, int aWidth)
            throws CircuitException {
        if (ctx.fx.cmp == null) throw new CircuitException("Comparator not found");
        List<Component> parts = new ArrayList<>();
        Map<String,Location> pins = new LinkedHashMap<>();

        AttributeSet ca = attrsWithWidthAndLabel(ctx.fx.cmp, aWidth, lbl(cell));
        Component cmp = add(ctx, ctx.fx.cmp, where, ca);
        parts.add(cmp);
        pins.put("Y", pinComparatorEQ(cmp));

        if (ctx.fx.constF != null) {
            AttributeSet ka = ctx.fx.constF.createAttributeSet();
            setByNameParsed(ka, "width", Integer.toString(aWidth));
            setByNameParsed(ka, "value", hexAllOnes(aWidth));
            parts.add(add(ctx, ctx.fx.constF, Location.create(where.getX()-40, where.getY()+10), ka));
        }
        return new Macro(cmp, parts, pins);
    }

    /** $reduce_xor / $reduce_xnor con Parity nativa. */
    public Macro buildReduceXorParity(ComposeCtx ctx, VerilogCell cell, Location where, int aWidth, boolean odd)
            throws CircuitException {
        ComponentFactory pf = odd ? ctx.fx.oddParityF : ctx.fx.evenParityF;
        if (pf == null) throw new CircuitException((odd?"Odd":"Even")+" Parity not found");
        List<Component> parts = new ArrayList<>();
        Map<String,Location> pins = new LinkedHashMap<>();
        AttributeSet pa = attrsWithWidthAndLabel(pf, aWidth, lbl(cell));
        Component par = add(ctx, pf, where, pa);
        parts.add(par);
        pins.put("Y", par.getLocation());
        return new Macro(par, parts, pins);
    }
}

