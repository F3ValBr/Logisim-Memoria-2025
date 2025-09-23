/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.arith;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Divider extends InstanceFactory {
	static final int PER_DELAY = 1;

    private static final int IN0   = 0; // dividend lower
    private static final int IN1   = 1; // divisor
    private static final int OUT   = 2; // quotient (low w bits)
    private static final int UPPER = 3; // dividend upper
    private static final int REM   = 4; // remainder (low w bits)

    // ===== Atributo de modo (Unsigned / Signed / Auto) =====
    public static final AttributeOption MODE_UNSIGNED
            = new AttributeOption("unsigned", "unsigned", Strings.getter("unsignedOption"));
    public static final AttributeOption MODE_SIGNED
            = new AttributeOption("signed", "signed",  Strings.getter("signedOption"));
    public static final AttributeOption MODE_AUTO
            = new AttributeOption("auto", "auto", Strings.getter("autoOption"));

    public static final Attribute<AttributeOption> SIGN_MODE =
            Attributes.forOption("signMode", Strings.getter("arithSignMode"),
                    new AttributeOption[]{ MODE_UNSIGNED, MODE_SIGNED, MODE_AUTO });

    // ===== Modo de división: Trunc o Floor =====
    public static final AttributeOption DIV_TRUNC
            = new AttributeOption("trunc", "trunc", Strings.getter("truncOption"));
    public static final AttributeOption DIV_FLOOR
            = new AttributeOption("floor", "floor", Strings.getter("floorOption"));

    public static final Attribute<AttributeOption> DIV_MODE =
            Attributes.forOption("divMode", Strings.getter("divMode"),
                    new AttributeOption[]{ DIV_TRUNC, DIV_FLOOR });


    public Divider() {
        super("Divider", Strings.getter("dividerComponent"));
        setAttributes(
                new Attribute[] { StdAttr.WIDTH, SIGN_MODE, DIV_MODE },
                new Object[]   { BitWidth.create(8), MODE_AUTO, DIV_TRUNC });
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));
        setIconName("divider.gif");

        Port[] ps = new Port[5];
        ps[IN0]   = new Port(-40, -10, Port.INPUT,  StdAttr.WIDTH);
        ps[IN1]   = new Port(-40,  10, Port.INPUT,  StdAttr.WIDTH);
        ps[OUT]   = new Port(  0,   0, Port.OUTPUT, StdAttr.WIDTH);
        ps[UPPER] = new Port(-20, -20, Port.INPUT,  StdAttr.WIDTH);
        ps[REM]   = new Port(-20,  20, Port.OUTPUT, StdAttr.WIDTH);
        ps[IN0].setToolTip(Strings.getter("dividerDividendLowerTip"));
        ps[IN1].setToolTip(Strings.getter("dividerDivisorTip"));
        ps[OUT].setToolTip(Strings.getter("dividerOutputTip"));
        ps[UPPER].setToolTip(Strings.getter("dividerDividendUpperTip"));
        ps[REM].setToolTip(Strings.getter("dividerRemainderTip"));
        setPorts(ps);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        super.configureNewInstance(instance);
        // Repropagar cuando cambie el modo o el ancho
        instance.getAttributeSet().addAttributeListener(new AttributeListener() {
            @Override public void attributeValueChanged(AttributeEvent e) {
                Attribute<?> a = e.getAttribute();
                if (a == SIGN_MODE || a == DIV_MODE) {
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
        AttributeOption signOpt = state.getAttributeValue(SIGN_MODE);
        AttributeOption divOpt  = state.getAttributeValue(DIV_MODE);

        // compute outputs
        Value lo    = state.getPort(IN0);
        Value den   = state.getPort(IN1);
        Value upper = state.getPort(UPPER);

        Value[] outs = computeResult(width, lo, den, upper, signOpt, divOpt);

        // propagate them
        int w = width.getWidth();
        int delay = w * (w + 2) * PER_DELAY;
        state.setPort(OUT, outs[0], delay);
        state.setPort(REM, outs[1], delay);
    }

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		painter.drawBounds();

		g.setColor(Color.GRAY);
		painter.drawPort(IN0);
		painter.drawPort(IN1);
		painter.drawPort(OUT);
		painter.drawPort(UPPER, Strings.get("dividerUpperInput"),  Direction.NORTH);
		painter.drawPort(REM, Strings.get("dividerRemainderOutput"), Direction.SOUTH);

        Location loc = painter.getLocation();
        int x = loc.getX(), y = loc.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.fillOval(x - 12, y - 7, 4, 4);
        g.drawLine(x - 15, y, x - 5, y);
        g.fillOval(x - 12, y + 3, 4, 4);
        GraphicsUtil.switchToWidth(g, 1);

        // Etiqueta de modo (U/S/A)
        try {
            AttributeOption mode = painter.getAttributeValue(SIGN_MODE);
            String tag = (mode == MODE_SIGNED) ? "S" : (mode == MODE_UNSIGNED ? "U" : "A");
            g.setColor(Color.DARK_GRAY);
            g.drawString(tag, x - 30, y + 5);
        } catch (Exception ignore) { }
    }

    /* ===================== Núcleo con modo ===================== */

    static Value[] computeResult(BitWidth width, Value lo, Value den, Value upper,
                                 AttributeOption signOpt, AttributeOption divOpt) {
        int w = width.getWidth();
        boolean upperDisconnected = (upper == Value.NIL || upper.isUnknown());

        // Si upper no está conectado, tratar como 0
        if (upperDisconnected) {
            upper = Value.createKnown(width, 0);
        }

        // Valores no definidos
        if (!(lo.isFullyDefined() && den.isFullyDefined() && upper.isFullyDefined())) {
            if (lo.isErrorValue() || den.isErrorValue() || upper.isErrorValue()) {
                return new Value[]{ Value.createError(width), Value.createError(width) };
            } else {
                return new Value[]{ Value.createUnknown(width), Value.createUnknown(width) };
            }
        }

        int loI    = lo.toIntValue();
        int upI    = upper.toIntValue();
        int denI   = den.toIntValue();

        boolean signed;
        if (signOpt == MODE_SIGNED) {
            signed = true;
        } else if (signOpt == MODE_UNSIGNED) {
            signed = false;
        } else { // AUTO
            if (upperDisconnected) {
                // usar solo LO + DEN para decidir
                signed = msbSet(loI, w) || msbSet(denI, w);
            } else {
                // usar UPPER + DEN
                signed = msbSet(upI, w) || msbSet(denI, w);
            }

        }

        long maskW = mask(w);
        long loU   = ((long) loI) & maskW;
        long upU   = ((long) upI) & maskW;

        long num, denL;
        if (signed) {
            // Dividendo 2w-bits con signo: (upper sign-extend) << w | (lo unsigned)
            long upS = signExtend(upI, w);
            num  = (upS << w) | loU;
            denL = signExtend(denI, w);
        } else {
            // Unsigned
            num  = (upU << w) | loU;
            denL = ((long) denI) & maskW;
        }

        if (denL == 0) denL = 1; // evita /0

        // División truncada base
        long q0 = num / denL; // trunc hacia 0
        long r0 = num % denL;

        if (divOpt == DIV_FLOOR) {
            // Ajuste a floor: si resto != 0 y los signos de num y den difieren
            if (r0 != 0 && ((num ^ denL) < 0)) {
                r0 += denL; // garantiza 0 <= r < |den|
                q0--;
            }
        }

        // Salida: bajo w bits
        long qOut = q0 & maskW;
        long rOut = r0 & maskW;

        return new Value[]{
                Value.createKnown(width, (int) qOut),
                Value.createKnown(width, (int) rOut)
        };
    }

    /* ===================== helpers numéricos ===================== */

    private static long mask(int w) {
        return (w >= 64) ? -1L : ((1L << w) - 1L);
    }

    private static boolean msbSet(int val, int w) {
        if (w <= 0) return false;
        int bit = 1 << (w - 1);
        return (val & bit) != 0;
    }

    private static long signExtend(int val, int w) {
        long u = ((long) val) & mask(w);
        long sign = 1L << (w - 1);
        return (u ^ sign) - sign;
    }
}
