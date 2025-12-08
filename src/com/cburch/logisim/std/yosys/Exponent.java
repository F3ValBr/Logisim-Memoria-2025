package com.cburch.logisim.std.yosys;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;
import java.math.BigInteger;

/** Exponent: OUT = (BASE ** EXP) mod 2^W
 *  - SIGN_MODE: Unsigned / Signed / Auto (Auto mira MSB(BASE))
 *  - EXP se interpreta SIEMPRE como UNSIGNED
 */
public class Exponent extends InstanceFactory {

    public static final String _ID = "Exponent";

    static final int PER_DELAY = 1;

    static final int IN0      = 0; // BASE
    static final int IN1      = 1; // EXP (unsigned)
    static final int OUT      = 2; // RESULT
    static final int SIGN_SEL = 3; // PIN opcional para seleccionar signo

    // ===== Modo de signo =====
    public static final AttributeOption MODE_UNSIGNED
            = new AttributeOption("unsigned", "unsigned", Strings.getter("unsignedOption"));
    public static final AttributeOption MODE_SIGNED
            = new AttributeOption("signed", "signed",  Strings.getter("signedOption"));
    public static final AttributeOption MODE_PIN
            = new AttributeOption("pin", "pin", Strings.getter("pinOption"));
    public static final AttributeOption MODE_AUTO
            = new AttributeOption("auto", "auto", Strings.getter("autoOption"));

    public static final Attribute<AttributeOption> SIGN_MODE =
            Attributes.forOption("signMode", Strings.getter("arithSignMode"),
                    new AttributeOption[]{ MODE_UNSIGNED, MODE_SIGNED, MODE_PIN, MODE_AUTO });

    public Exponent() {
        super(_ID, Strings.getter("exponentComponent"));
        setAttributes(
                new Attribute[]{ StdAttr.WIDTH, SIGN_MODE },
                new Object[]  { BitWidth.create(8), MODE_UNSIGNED }
        );
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));
        setIconName("exponent.gif");
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        updatePorts(instance);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == SIGN_MODE || attr == StdAttr.WIDTH) {
            updatePorts(instance);
            instance.recomputeBounds();
            instance.fireInvalidated();
        }
    }

    /* ===== helpers de modo de signo ===== */

    private static boolean pinModeEnabled(Instance instance) {
        AttributeOption mode = instance.getAttributeValue(SIGN_MODE);
        return mode == MODE_PIN;
    }

    private void updatePorts(Instance instance) {
        BitWidth w = instance.getAttributeValue(StdAttr.WIDTH);
        boolean pinMode = pinModeEnabled(instance);

        Port base = new Port(-40, -10, Port.INPUT,  w);          // IN0
        Port exp  = new Port(-40,  10, Port.INPUT,  w);          // IN1 (unsigned)
        Port out  = new Port(  0,   0, Port.OUTPUT, w);          // OUT

        base.setToolTip(Strings.getter("exponentBaseTip"));
        exp.setToolTip(Strings.getter("exponentExponentTip"));
        out.setToolTip(Strings.getter("exponentOutputTip"));

        if (pinMode) {
            Port signSel = new Port(-30, 20, Port.INPUT, BitWidth.ONE); // SIGN_SEL
            signSel.setToolTip(Strings.getter("exponentSignSelTip"));
            instance.setPorts(new Port[]{ base, exp, out, signSel });
        } else {
            instance.setPorts(new Port[]{ base, exp, out });
        }

        instance.fireInvalidated();
    }

    @Override
    public void propagate(InstanceState state) {
        BitWidth w = state.getAttributeValue(StdAttr.WIDTH);
        AttributeOption signOpt = state.getAttributeValue(SIGN_MODE);

        Value base = state.getPort(IN0);
        Value exp  = state.getPort(IN1);

        boolean signed = decideSigned(state, signOpt, w, base);
        Value out = computePow(w, base, exp, signed);

        int delay = Math.max(1, (w.getWidth() + 2) * PER_DELAY);
        state.setPort(OUT, out, delay);
    }

    /**
     * Decide si la BASE se interpreta como signed o unsigned, según:
     * - SIGNED  → siempre signed
     * - UNSIGNED→ siempre unsigned
     * - PIN     → lee SIGN_SEL (1 => signed)
     * - AUTO    → mira MSB(BASE)
     */
    private static boolean decideSigned(InstanceState st,
                                        AttributeOption signOpt,
                                        BitWidth w,
                                        Value base) {
        if (signOpt == MODE_SIGNED)   return true;
        if (signOpt == MODE_UNSIGNED) return false;

        if (signOpt == MODE_PIN) {
            try {
                Value sel = st.getPort(SIGN_SEL);
                return sel == Value.TRUE; // 1 => signed, 0/X/NC => unsigned
            } catch (IndexOutOfBoundsException ex) {
                return false;
            }
        }

        // AUTO: signed si MSB(base)==1
        int width = w.getWidth();
        if (!base.isFullyDefined() || width <= 0) return false;
        Value[] bits = base.getAll();
        return bits[width - 1] == Value.TRUE;
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        painter.drawBounds();

        AttributeOption m = painter.getAttributeValue(SIGN_MODE);
        boolean pinMode = (m == MODE_PIN);

        g.setColor(Color.GRAY);
        painter.drawPort(IN0);          // BASE
        painter.drawPort(IN1);          // EXP
        painter.drawPort(OUT);          // RESULT
        if (pinMode) {
            painter.drawPort(SIGN_SEL); // pin de signo si existe
        }
        g.setColor(Color.BLACK);

        // Dibujito simple de "^"
        Location loc = painter.getLocation();
        int x = loc.getX(), y = loc.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.drawLine(x - 15, y,     x - 10, y - 5);
        g.drawLine(x - 10, y - 5, x - 5,  y);
        GraphicsUtil.switchToWidth(g, 1);

        // Marca de modo U/S/P/A
        try {
            String tag =
                    (m == MODE_SIGNED)   ? "S" :
                            (m == MODE_UNSIGNED) ? "U" :
                                    (m == MODE_PIN)      ? "P" : "A";
            g.setColor(Color.DARK_GRAY);
            g.drawString(tag, x - 30, y + 5);
        } catch (Exception ignore) { }
        g.setColor(Color.BLACK);
    }

    /* =================== Núcleo =================== */
    static Value computePow(BitWidth width, Value base, Value exp, boolean signed) {
        int w = width.getWidth();

        // Errores/unknowns
        if (base.isErrorValue() || exp.isErrorValue())
            return Value.createError(width);
        if (!(base.isFullyDefined() && exp.isFullyDefined()))
            return Value.createUnknown(width);

        // Módulo 2^W y máscara (como BigInteger)
        BigInteger MOD   = BigInteger.ONE.shiftLeft(w);          // 2^W
        BigInteger MASK  = MOD.subtract(BigInteger.ONE);         // 2^W - 1

        // EXP siempre UNSIGNED (Yosys estándar)
        BigInteger e = bigUnsigned(exp, w);

        // BASE según el modo
        BigInteger a = signed ? bigSigned(base, w) : bigUnsigned(base, w);

        // Casos rápidos
        if (e.signum() == 0) {
            // x^0 = 1
            return Value.createKnown(width, 1);
        }
        // Nota: con wrap mod 2^W, 0^e = 0 para e>0
        if (a.and(MASK).signum() == 0) {
            return Value.createKnown(width, 0);
        }

        // Exponenciación rápida (square & multiply) con wrap 2^W
        BigInteger res     = BigInteger.ONE;
        BigInteger baseAcc = a.and(MASK); // reducir a W bits
        BigInteger ee      = e;

        while (ee.signum() > 0) {
            if (ee.testBit(0)) {
                res = res.multiply(baseAcc).and(MASK); // == mod 2^W
            }
            ee = ee.shiftRight(1);
            if (ee.signum() > 0) {
                baseAcc = baseAcc.multiply(baseAcc).and(MASK);
            }
        }

        return Value.createKnown(width, res.intValue());
    }

    /** Lee Value (definido) como BigInteger UNSIGNED (bits tal cual). */
    private static BigInteger bigUnsigned(Value v, int w) {
        Value[] bits = v.getAll();
        BigInteger acc = BigInteger.ZERO;
        for (int i = 0; i < w; i++) {
            if (bits[i] == Value.TRUE) {
                acc = acc.setBit(i);
            }
        }
        return acc;
    }

    /** Lee Value (definido) como BigInteger SIGNED en two's complement, rango [-2^(w-1), 2^(w-1)-1]. */
    private static BigInteger bigSigned(Value v, int w) {
        BigInteger u = bigUnsigned(v, w);
        BigInteger signBit = BigInteger.ONE.shiftLeft(w - 1);
        if (u.testBit(w - 1)) {
            // valor negativo: u - 2^W
            return u.subtract(BigInteger.ONE.shiftLeft(w));
        } else {
            return u;
        }
    }
}
