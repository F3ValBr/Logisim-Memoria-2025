package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Timer extends InstanceFactory {

    public static final String _ID = "Timer";

    // Modo de medición: real vs virtual (clock externo)
    private static final AttributeOption MODE_REAL =
            new AttributeOption("real", "real", Strings.getter("timerModeReal"));
    private static final AttributeOption MODE_VIRTUAL =
            new AttributeOption("virtual", "virtual", Strings.getter("timerModeVirtual"));

    // Atributo para elegir modo
    public static final Attribute<AttributeOption> ATTR_MODE =
            Attributes.forOption("mode", Strings.getter("timerModeAttr"),
                    new AttributeOption[]{ MODE_REAL, MODE_VIRTUAL });

    // Índices lógicos de puertos
    private static final int OUT_TIME = 0; // salida T[31:0]
    private static final int IN_SEL   = 1; // entrada “select / trigger”
    private static final int IN_CLK   = 2; // entrada “clock” (sólo existe en modo virtual)

    private static final BitWidth BW32 = BitWidth.create(32);

    public Timer() {
        super(_ID, Strings.getter("timerComponent"));
        setOffsetBounds(Bounds.create(-30, -20, 30, 40));
        setIconName("timer.gif");

        setAttributes(
                new Attribute[]{
                        ATTR_MODE,
                        StdAttr.LABEL,
                        StdAttr.LABEL_FONT
                },
                new Object[]{
                        MODE_REAL,
                        "",
                        StdAttr.DEFAULT_LABEL_FONT
                }
        );

        // Puertos base (se ajustarán en configureNewInstance)
        Port[] ps = new Port[3];
        ps[OUT_TIME] = new Port(  0,   0, Port.OUTPUT, 32);
        ps[IN_SEL]   = new Port(-30,   0, Port.INPUT,  1);
        ps[IN_CLK]   = new Port(-30,  10, Port.INPUT,  1);
        setPorts(ps);
    }

    /* ===================== PUERTOS DINÁMICOS ===================== */

    @Override
    protected void configureNewInstance(Instance instance) {
        recomputePorts(instance);

        Bounds bds = instance.getBounds();
        instance.setTextField(
                StdAttr.LABEL,
                StdAttr.LABEL_FONT,
                bds.getX() + bds.getWidth() / 2,
                bds.getY() - 3,
                GraphicsUtil.H_CENTER,
                GraphicsUtil.V_BASELINE
        );

        instance.addAttributeListener();
    }

    @Override
    public void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == ATTR_MODE) {
            recomputePorts(instance);
        }
    }

    private void recomputePorts(Instance instance) {
        AttributeOption mode = instance.getAttributeValue(ATTR_MODE);

        List<Port> list = new ArrayList<>();

        // OUT (T)
        Port pOut = new Port(0, 0, Port.OUTPUT, 32);
        pOut.setToolTip(Strings.getter("timerOutTip"));
        list.add(pOut);

        // IN_SEL (S)
        Port pSel = new Port(-30, 0, Port.INPUT, 1);
        pSel.setToolTip(Strings.getter("timerSelTip"));
        list.add(pSel);

        // IN_CLK (solo en modo virtual)
        if (mode == MODE_VIRTUAL) {
            Port pClk = new Port(-30, 10, Port.INPUT, 1);
            pClk.setToolTip(Strings.getter("timerClkTip"));
            list.add(pClk);
        }

        instance.setPorts(list.toArray(new Port[0]));
        instance.recomputeBounds();

        Bounds b = instance.getBounds();
        instance.setTextField(
                StdAttr.LABEL,
                StdAttr.LABEL_FONT,
                b.getX() + b.getWidth() / 2,
                b.getY() - 3,
                GraphicsUtil.H_CENTER,
                GraphicsUtil.V_BASELINE
        );
    }

    /* ======================== DIBUJO ======================== */

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Bounds b   = painter.getBounds();

        painter.drawBounds();
        painter.drawLabel();

        AttributeOption mode = painter.getAttributeValue(ATTR_MODE);

        // Puertos
        painter.drawPort(OUT_TIME, "T", Direction.WEST);

        g.setColor(Color.GRAY);
        painter.drawPort(IN_SEL, "S", Direction.EAST);
        g.setColor(Color.BLACK);

        if (mode == MODE_VIRTUAL) {
            painter.drawClock(IN_CLK, Direction.EAST);
        }

        // Reloj de arena (X)
        int x1 = b.getX() + 4;
        int y1 = b.getY() + 4;
        int x2 = b.getX() + b.getWidth()  - 4;
        int y2 = b.getY() + b.getHeight() - 4;

        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x1, y2, x2, y1);

        // Tag "R" o "V"
        String tag = (mode == MODE_VIRTUAL) ? "V" : "R";
        g.setColor(Color.DARK_GRAY);
        GraphicsUtil.drawText(g, tag,
                b.getX() + b.getWidth() / 2,
                b.getY() + b.getHeight() / 8,
                GraphicsUtil.H_CENTER,
                GraphicsUtil.V_CENTER);
        g.setColor(Color.BLACK);
    }

    /* ===================== SIMULACIÓN ===================== */

    @Override
    public void propagate(InstanceState state) {
        TimerData data = (TimerData) state.getData();
        if (data == null) {
            data = new TimerData();
            state.setData(data);
        }

        AttributeOption mode = state.getAttributeValue(ATTR_MODE);

        Value sel = state.getPort(IN_SEL);
        boolean selNow  = (sel == Value.TRUE);
        boolean selPrev = (data.lastSelect == Value.TRUE);
        boolean selRise = selNow && !selPrev;

        if (mode == MODE_VIRTUAL) {

            Value clk = state.getPort(IN_CLK);
            boolean clkNow  = (clk == Value.TRUE);
            boolean clkPrev = (data.lastClock == Value.TRUE);
            boolean clkRise = clkNow && !clkPrev;

            if (clkRise) data.virtualTicks++;

            if (selRise) {
                long delta = data.virtualTicks - data.lastVirtualAtTrigger;
                if (delta < 0) delta = 0;
                state.setPort(OUT_TIME, Value.createKnown(BW32, (int) delta), 0);
                data.lastVirtualAtTrigger = data.virtualTicks;
            }

            data.lastClock = clk;

        } else {
            long now = System.currentTimeMillis();

            if (selRise) {
                long delta = (data.lastRealTime == 0L) ? 0 : now - data.lastRealTime;
                if (delta < 0) delta = 0;
                state.setPort(OUT_TIME, Value.createKnown(BW32, (int) delta), 0);
                data.lastRealTime = now;
            } else if (data.lastRealTime == 0L && selNow) {
                data.lastRealTime = now;
            }
        }

        data.lastSelect = sel;
    }
}
