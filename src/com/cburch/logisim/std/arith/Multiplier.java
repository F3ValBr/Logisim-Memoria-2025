/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.arith;

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Multiplier extends InstanceFactory {
	static final int PER_DELAY = 1;

	private static final int IN0   = 0;
	private static final int IN1   = 1;
	private static final int OUT   = 2;
	private static final int C_IN  = 3;
	private static final int C_OUT = 4;

    // === Atributo de modo de signo ===
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

    public Multiplier() {
        super("Multiplier", Strings.getter("multiplierComponent"));

        setAttributes(
                new Attribute[]{ StdAttr.WIDTH, SIGN_MODE },
                new Object[]   { BitWidth.create(8), MODE_AUTO }
        );
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));
        setIconName("multiplier.gif");

		Port[] ps = new Port[5];
		ps[IN0]   = new Port(-40, -10, Port.INPUT,  StdAttr.WIDTH);
		ps[IN1]   = new Port(-40,  10, Port.INPUT,  StdAttr.WIDTH);
		ps[OUT]   = new Port(  0,   0, Port.OUTPUT, StdAttr.WIDTH);
		ps[C_IN]  = new Port(-20, -20, Port.INPUT,  StdAttr.WIDTH);
		ps[C_OUT] = new Port(-20,  20, Port.OUTPUT, StdAttr.WIDTH);
		ps[IN0].setToolTip(Strings.getter("multiplierInputTip"));
		ps[IN1].setToolTip(Strings.getter("multiplierInputTip"));
		ps[OUT].setToolTip(Strings.getter("multiplierOutputTip"));
		ps[C_IN].setToolTip(Strings.getter("multiplierCarryInTip"));
		ps[C_OUT].setToolTip(Strings.getter("multiplierCarryOutTip"));
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
            @Override public void attributeListChanged(AttributeEvent e) { /* no-op */ }
        });
    }

    @Override
    public void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == SIGN_MODE) {
            instance.fireInvalidated();
        } else if (attr == StdAttr.WIDTH) {
            instance.recomputeBounds();
            instance.fireInvalidated();
        }
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

        Value[] outs = computeProduct(width, a, b, c_in, modeOpt);

        // propagate them
        int w = width.getWidth();
        int delay = w * (w + 2) * PER_DELAY;
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
		painter.drawPort(C_OUT, "c out", Direction.SOUTH);

        // Icono "X"
        Location loc = painter.getLocation();
        int x = loc.getX();
        int y = loc.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.drawLine(x - 15, y - 5, x - 5, y + 5);
        g.drawLine(x - 15, y + 5, x - 5, y - 5);
        GraphicsUtil.switchToWidth(g, 1);

        // Etiqueta de modo (pequeña)
        try {
            AttributeOption mode = painter.getAttributeValue(SIGN_MODE);
            String tag = (mode == MODE_SIGNED) ? "S" : (mode == MODE_UNSIGNED ? "U" : "A");
            g.setColor(Color.DARK_GRAY);
            g.drawString(tag, x - 30, y + 5);
        } catch (Exception ignore) {}
    }

    /* ===================== Núcleo de cálculo con modo ===================== */

    private static Value[] computeProduct(BitWidth width, Value a, Value b, Value c_in,
                                          AttributeOption modeOpt) {
        int w = width.getWidth();
        if (c_in == Value.NIL || c_in.isUnknown()) c_in = Value.createKnown(width, 0);

        // Si hay indefinidos/errores, conserva el comportamiento original
        if (!(a.isFullyDefined() && b.isFullyDefined() && c_in.isFullyDefined())) {
            return computeProductUnknown(width, a, b, c_in);
        }

        // Interpretar entradas según el modo
        int ai = a.toIntValue();
        int bi = b.toIntValue();
        int ci = c_in.toIntValue();

        boolean signed;
        if (modeOpt == MODE_SIGNED) {
            signed = true;
        } else if (modeOpt == MODE_UNSIGNED) {
            signed = false;
        } else { // MODE_AUTO
            signed = msbSet(ai, w) || msbSet(bi, w);
        }

        long av = signed ? signExtend(ai, w) : unsigned(ai, w);
        long bv = signed ? signExtend(bi, w) : unsigned(bi, w);
        long cv = signed ? signExtend(ci, w) : unsigned(ci, w);

        // Producto + c_in (en 2w bits conceptuales)
        long prod = av * bv + cv;

        long maskW = mask(w);
        long lo = prod & maskW;
        long hi = (prod >> w) & maskW;

        return new Value[]{
                Value.createKnown(width, (int) lo),
                Value.createKnown(width, (int) hi)
        };
    }

    // Mantiene el comportamiento original para UNKNOWN/ERROR
    private static Value[] computeProductUnknown(BitWidth width, Value a, Value b, Value c_in) {
        int w = width.getWidth();
        Value[] avals = a.getAll();
        int aOk = findUnknown(avals);
        int aErr = findError(avals);
        int ax = getKnown(avals);
        Value[] bvals = b.getAll();
        int bOk = findUnknown(bvals);
        int bErr = findError(bvals);
        int bx = getKnown(bvals);
        Value[] cvals = c_in.getAll();
        int cOk = findUnknown(cvals);
        int cErr = findError(cvals);
        int cx = getKnown(cvals);

        int known = Math.min(Math.min(aOk, bOk), cOk);
        int error = Math.min(Math.min(aErr, bErr), cErr);
        int ret = ax * bx + cx;

        Value[] bits = new Value[w];
        for (int i = 0; i < w; i++) {
            if (i < known) {
                bits[i] = ((ret & (1 << i)) != 0 ? Value.TRUE : Value.FALSE);
            } else if (i < error) {
                bits[i] = Value.UNKNOWN;
            } else {
                bits[i] = Value.ERROR;
            }
        }
        return new Value[]{
                Value.create(bits),
                error < w ? Value.createError(width) : Value.createUnknown(width)
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

    private static long unsigned(int val, int w) {
        return ((long) val) & mask(w);
    }

    private static long signExtend(int val, int w) {
        long u = ((long) val) & mask(w);
        long sign = 1L << (w - 1);
        return (u ^ sign) - sign; // ext. de signo clásica
    }

    private static int findUnknown(Value[] vals) {
        for (int i = 0; i < vals.length; i++) {
            if (!vals[i].isFullyDefined()) return i;
        }
        return vals.length;
    }

    private static int findError(Value[] vals) {
        for (int i = 0; i < vals.length; i++) {
            if (vals[i].isErrorValue()) return i;
        }
        return vals.length;
    }

    private static int getKnown(Value[] vals) {
        int ret = 0;
        for (int i = 0; i < vals.length; i++) {
            int v = vals[i].toIntValue();
            if (v < 0) return ret;
            ret |= v << i;
        }
        return ret;
    }
}
