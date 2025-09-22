/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.arith;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Adder extends InstanceFactory {
	static final int PER_DELAY = 1;

    private static final int IN0   = 0;
    private static final int IN1   = 1;
    private static final int OUT   = 2;
    private static final int C_IN  = 3;
    private static final int C_OUT = 4;

    // ===== Modo de signo =====
    // Tres opciones: Unsigned / Signed / Auto
    public static final AttributeOption MODE_UNSIGNED
            = new AttributeOption("unsigned", "unsigned", Strings.getter("unsignedOption"));
    public static final AttributeOption MODE_SIGNED
            = new AttributeOption("signed", "signed",  Strings.getter("signedOption"));
    public static final AttributeOption MODE_AUTO
            = new AttributeOption("auto", "auto", Strings.getter("autoOption"));
    public static final Attribute<AttributeOption> SIGN_MODE =
            Attributes.forOption("signMode", Strings.getter("arithSignMode"),
                    new AttributeOption[]{ MODE_UNSIGNED, MODE_SIGNED, MODE_AUTO });

    public Adder() {
        super("Adder", Strings.getter("adderComponent"));
        setAttributes(
                new Attribute[]{ StdAttr.WIDTH, SIGN_MODE },
                new Object[]   { BitWidth.create(8), MODE_AUTO }
        );
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));
        setIconName("adder.gif");

        Port[] ps = new Port[5];
        ps[IN0]   = new Port(-40, -10, Port.INPUT,  StdAttr.WIDTH);
        ps[IN1]   = new Port(-40,  10, Port.INPUT,  StdAttr.WIDTH);
        ps[OUT]   = new Port(  0,   0, Port.OUTPUT, StdAttr.WIDTH);
        ps[C_IN]  = new Port(-20, -20, Port.INPUT,  1);
        ps[C_OUT] = new Port(-20,  20, Port.OUTPUT, 1);
        ps[IN0].setToolTip(Strings.getter("adderInputTip"));
        ps[IN1].setToolTip(Strings.getter("adderInputTip"));
        ps[OUT].setToolTip(Strings.getter("adderOutputTip"));
        ps[C_IN].setToolTip(Strings.getter("adderCarryInTip"));
        ps[C_OUT].setToolTip(Strings.getter("adderCarryOutTip"));
        setPorts(ps);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        super.configureNewInstance(instance);
        instance.getAttributeSet().addAttributeListener(new AttributeListener() {
            @Override public void attributeValueChanged(AttributeEvent e) {
                Attribute<?> a = e.getAttribute();
                if (a == SIGN_MODE) {
                    instance.fireInvalidated();
                } else if (a == StdAttr.WIDTH) {
                    instance.recomputeBounds();
                    instance.fireInvalidated();
                }
            }
            @Override public void attributeListChanged(AttributeEvent e) { }
        });
    }

    @Override
    public void propagate(InstanceState state) {
        // get attributes
        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
        AttributeOption modeOpt = state.getAttributeValue(SIGN_MODE);

        // compute outputs
        Value a    = state.getPort(IN0);
        Value b    = state.getPort(IN1);
        Value c_in = state.getPort(C_IN);

        Value[] outs = computeSum(width, a, b, c_in, modeOpt);

        // propagate them
        int delay = (width.getWidth() + 2) * PER_DELAY;
        state.setPort(OUT,   outs[0], delay);
        state.setPort(C_OUT, outs[1], delay);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        painter.drawBounds();

        g.setColor(Color.GRAY);
        painter.drawPort(IN0);
        painter.drawPort(IN1);
        painter.drawPort(OUT);
        painter.drawPort(C_IN,  "c in",  Direction.NORTH);
        AttributeOption m = painter.getAttributeValue(SIGN_MODE);
        String coutLbl = (m == MODE_SIGNED) ? "ovf" : (m == MODE_UNSIGNED ? "c out" : "c/ovf");
        painter.drawPort(C_OUT, coutLbl, Direction.SOUTH); // carry u overflow

        Location loc = painter.getLocation();
        int x = loc.getX();
        int y = loc.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.drawLine(x - 15, y, x - 5, y);
        g.drawLine(x - 10, y - 5, x - 10, y + 5);
        GraphicsUtil.switchToWidth(g, 1);

        // Marca de modo: U/S/A
        try {
            String tag = (m == MODE_SIGNED) ? "S" : (m == MODE_UNSIGNED ? "U" : "A");
            g.setColor(Color.DARK_GRAY);
            g.drawString(tag, x - 30, y + 5);
        } catch (Exception ignore) {}
    }

    /* ====================== Núcleo ====================== */
    static Value[] computeSum(BitWidth width, Value a, Value b, Value c_in,
                              AttributeOption modeOpt) {
        int w = width.getWidth();
        if (c_in == Value.UNKNOWN || c_in == Value.NIL) c_in = Value.FALSE;

        // Camino rápido: totalmente definidos
        if (a.isFullyDefined() && b.isFullyDefined() && c_in.isFullyDefined()) {
            int ai = a.toIntValue();
            int bi = b.toIntValue();
            int ci = c_in.toIntValue() & 1;

            boolean signed;
            if (modeOpt == MODE_SIGNED) {
                signed = true;
            } else if (modeOpt == MODE_UNSIGNED) {
                signed = false;
            } else { // AUTO: signed si MSB(A) o MSB(B)
                signed = msbSet(ai, w) || msbSet(bi, w);
            }

            long mask = (w >= 64) ? -1L : ((1L << w) - 1L);

            if (!signed) {
                // ===== Unsigned: carry-out real
                long av = ((long) ai) & mask;
                long bv = ((long) bi) & mask;
                long sum = av + bv + ci;
                int  out = (int) (sum & mask);
                Value carry = (((sum >>> w) & 1L) != 0) ? Value.TRUE : Value.FALSE;
                return new Value[]{ Value.createKnown(width, out), carry };
            } else {
                // ===== Signed: overflow (two's complement)
                long min = -(1L << (w - 1));
                long max =  (1L << (w - 1)) - 1;

                long av = signExtend(ai, w);
                long bv = signExtend(bi, w);
                long sum = av + bv + ci;

                int  out = (int) (sum & mask);
                boolean ovf = (sum < min) || (sum > max);
                Value ovfBit = ovf ? Value.TRUE : Value.FALSE;
                return new Value[]{ Value.createKnown(width, out), ovfBit };
            }
        }

        // Camino bit-a-bit (UNKNOWN/ERROR): conserva tu semántica previa (carry)
        Value[] bits = new Value[w];
        Value carry = c_in;
        for (int i = 0; i < w; i++) {
            if (carry == Value.ERROR) {
                bits[i] = Value.ERROR;
            } else if (carry == Value.UNKNOWN) {
                bits[i] = Value.UNKNOWN;
            } else {
                Value ab = a.get(i);
                Value bb = b.get(i);
                if (ab == Value.ERROR || bb == Value.ERROR) {
                    bits[i] = Value.ERROR; carry = Value.ERROR;
                } else if (ab == Value.UNKNOWN || bb == Value.UNKNOWN) {
                    bits[i] = Value.UNKNOWN; carry = Value.UNKNOWN;
                } else {
                    int s = (ab == Value.TRUE ? 1 : 0)
                            + (bb == Value.TRUE ? 1 : 0)
                            + (carry == Value.TRUE ? 1 : 0);
                    bits[i] = ((s & 1) == 1) ? Value.TRUE : Value.FALSE;
                    carry = (s >= 2) ? Value.TRUE : Value.FALSE;
                }
            }
        }
        return new Value[]{ Value.create(bits), carry };
    }

    /* ====================== helpers ====================== */
    private static boolean msbSet(int val, int w) {
        if (w <= 0) return false;
        int bit = 1 << (w - 1);
        return (val & bit) != 0;
    }

    private static long signExtend(int val, int w) {
        long mask = (w >= 64) ? -1L : ((1L << w) - 1L);
        long u = ((long) val) & mask;
        long sign = 1L << (w - 1);
        return (u ^ sign) - sign;
    }
}

