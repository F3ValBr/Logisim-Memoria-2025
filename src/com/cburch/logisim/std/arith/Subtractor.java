/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.arith;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Subtractor extends InstanceFactory {
    private static final int IN0   = 0; // A (minuend)
    private static final int IN1   = 1; // B (subtrahend)
    private static final int OUT   = 2; // Y = A - B - B_IN
    private static final int B_IN  = 3; // borrow in
    private static final int B_OUT = 4; // borrow out (unsigned) / overflow (signed)

    // ===== Modo de signo =====
    public static final AttributeOption MODE_UNSIGNED
            = new AttributeOption("unsigned", "unsigned", Strings.getter("unsignedOption"));
    public static final AttributeOption MODE_SIGNED
            = new AttributeOption("signed", "signed",  Strings.getter("signedOption"));
    public static final AttributeOption MODE_AUTO
            = new AttributeOption("auto", "auto", Strings.getter("autoOption"));
    public static final Attribute<AttributeOption> SIGN_MODE =
            Attributes.forOption("signMode", Strings.getter("arithSignMode"),
                    new AttributeOption[]{ MODE_UNSIGNED, MODE_SIGNED, MODE_AUTO });

    public Subtractor() {
        super("Subtractor", Strings.getter("subtractorComponent"));
        setAttributes(
                new Attribute[] { StdAttr.WIDTH, SIGN_MODE },
                new Object[]  { BitWidth.create(8), MODE_AUTO }
        );
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));
        setIconName("subtractor.gif");

        Port[] ps = new Port[5];
        ps[IN0]   = new Port(-40, -10, Port.INPUT,  StdAttr.WIDTH);
        ps[IN1]   = new Port(-40,  10, Port.INPUT,  StdAttr.WIDTH);
        ps[OUT]   = new Port(  0,   0, Port.OUTPUT, StdAttr.WIDTH);
        ps[B_IN]  = new Port(-20, -20, Port.INPUT,  1);
        ps[B_OUT] = new Port(-20,  20, Port.OUTPUT, 1);
        ps[IN0].setToolTip(Strings.getter("subtractorMinuendTip"));
        ps[IN1].setToolTip(Strings.getter("subtractorSubtrahendTip"));
        ps[OUT].setToolTip(Strings.getter("subtractorOutputTip"));
        ps[B_IN].setToolTip(Strings.getter("subtractorBorrowInTip"));
        ps[B_OUT].setToolTip(Strings.getter("subtractorBorrowOutTip")); // en signed será overflow
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
        Value b_in = state.getPort(B_IN);
        if (b_in == Value.UNKNOWN || b_in == Value.NIL) b_in = Value.FALSE;

        // A - B - B_IN  =  A + (~B) + (~B_IN)
        Value[] sum = Adder.computeSum(width, a, b.not(), b_in.not(), modeOpt);

        // propagate them
        // OUT siempre es la suma anterior
        int delay = (width.getWidth() + 4) * Adder.PER_DELAY;
        state.setPort(OUT, sum[0], delay);

        // B_OUT depende del modo:
        //  - Unsigned: BorrowOut = NOT(CarryOut)
        //  - Signed:   Overflow  = Adder.C_OUT (ya es overflow en modo signed)
        Value bout;
        boolean signed;
        if (modeOpt == MODE_SIGNED) {
            signed = true;
        } else if (modeOpt == MODE_UNSIGNED) {
            signed = false;
        } else {
            // AUTO: signed si MSB(A) o MSB(B)
            int w = width.getWidth();
            signed = msbSet(a.toIntValue(), w) || msbSet(b.toIntValue(), w);
        }
        if (signed) {
            bout = sum[1];           // overflow (tal como entrega Adder en modo signed)
        } else {
            bout = sum[1].not();     // borrow = NOT(carry) en resta unsigned por identidad de 2C
        }

        state.setPort(B_OUT, bout, delay);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        painter.drawBounds();

        g.setColor(Color.GRAY);
        painter.drawPort(IN0);
        g.setColor(Color.GRAY);
        painter.drawPort(IN1);
        painter.drawPort(OUT);
        painter.drawPort(B_IN,  "b in",  Direction.NORTH);

        // Etiqueta dinámica para B_OUT: "b/ovf"
        AttributeOption m = painter.getAttributeValue(SIGN_MODE);
        String boutLbl = (m == MODE_SIGNED) ? "ovf" : (m == MODE_UNSIGNED ? "b out" : "b/ovf");
        painter.drawPort(B_OUT, boutLbl, Direction.SOUTH);

        Location loc = painter.getLocation();
        int x = loc.getX();
        int y = loc.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.drawLine(x - 15, y, x - 5, y); // símbolo "−"
        GraphicsUtil.switchToWidth(g, 1);

        // Marca de modo: U/S/A
        try {
            String tag = (m == MODE_SIGNED) ? "S" : (m == MODE_UNSIGNED ? "U" : "A");
            g.setColor(Color.DARK_GRAY);
            g.drawString(tag, x - 30, y + 5);
        } catch (Exception ignore) {}
    }

    /* ===== Helpers ===== */
    private static boolean msbSet(int val, int w) {
        if (w <= 0) return false;
        int bit = 1 << (w - 1);
        return (val & bit) != 0;
    }
}
