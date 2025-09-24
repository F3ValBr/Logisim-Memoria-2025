package com.cburch.logisim.std.plexers;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;

import static com.cburch.logisim.data.Direction.*;

public class PriorityMultiplexer extends InstanceFactory {

    /** Límite de bits por puerto en Logisim */
    private static final int MAX_BUS  = 32;
    /** Límite capacidad total */
    private static final int MAX_AGG  = 64;

    // Índices lógicos según haya puerto extra
    private static int idxA (boolean extra){ return 0; }
    private static int idxB (boolean extra){ return 1; }
    private static int idxBX(boolean extra){ return extra ? 2 : -1; }
    private static int idxS (boolean extra){ return extra ? 3 : 2; }
    private static int idxY (boolean extra){ return extra ? 4 : 3; }

    // Atributo para el ancho del selector (S_WIDTH)
    public static final Attribute<BitWidth> ATTR_SWIDTH =
            Attributes.forBitWidth("swidth", Strings.getter("priorityMuxSWidth"), 1, 8);

    public PriorityMultiplexer() {
        super("Priority Multiplexer", Strings.getter("pmuxComponent"));
        setAttributes(
                new Attribute[]{ StdAttr.FACING, StdAttr.WIDTH, ATTR_SWIDTH },
                new Object[]   { Direction.EAST, BitWidth.create(8), BitWidth.create(2) }
        );
        setFacingAttribute(StdAttr.FACING);
        setKeyConfigurator(JoinedConfigurator.create(
                new BitWidthConfigurator(ATTR_SWIDTH, 1, 8, 0),
                new BitWidthConfigurator(StdAttr.WIDTH)
        ));
        setIconName("pmultiplexer.gif");
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attrs) {
        Direction dir = attrs.getValue(StdAttr.FACING);
        Bounds base = Bounds.create(-40, -20, 40, 40);
        return base.rotate(Direction.EAST, dir, 0, 0);
    }

    @Override
    public boolean contains(Location loc, AttributeSet attrs) {
        Direction facing = attrs.getValue(StdAttr.FACING);
        return Plexers.contains(loc, getOffsetBounds(attrs), facing);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        updatePorts(instance);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
        if (attr == StdAttr.FACING) {
            instance.recomputeBounds();
            updatePorts(instance);
        } else if (attr == StdAttr.WIDTH || attr == ATTR_SWIDTH) {
            updatePorts(instance);
        }
    }

    private void updatePorts(Instance instance) {
        Direction dir = instance.getAttributeValue(StdAttr.FACING);
        BitWidth w  = instance.getAttributeValue(StdAttr.WIDTH);
        BitWidth sw = instance.getAttributeValue(ATTR_SWIDTH);

        // Cap total a 64; luego partir en <=32 + resto<=32
        int cap     = bWidth(w, sw);
        int bMain   = Math.min(cap, MAX_BUS);
        int bExtra  = Math.max(0, cap - bMain);
        boolean hasExtra = bExtra > 0;

        // Posiciones
        Location a, b, bx, s, y;
        if (dir == Direction.WEST) {
            a  = Location.create( 40, -10);
            b  = Location.create( 40,   0);
            bx = Location.create( 40,  10);
            s  = Location.create( 30, 20);
            y  = Location.create(  0,   0);
        } else if (dir == Direction.NORTH) {
            a  = Location.create(-10,  40);
            b  = Location.create(  0,  40);
            bx = Location.create( 10,  40);
            s  = Location.create(-20,  30);
            y  = Location.create(  0,   0);
        } else if (dir == Direction.SOUTH) {
            a  = Location.create(-10, -40);
            b  = Location.create(  0, -40);
            bx = Location.create( 10, -40);
            s  = Location.create(-20, -30);
            y  = Location.create(  0,   0);
        } else { // EAST
            a  = Location.create(-40, -10);
            b  = Location.create(-40,   0);
            bx = Location.create(-40,  10);
            s  = Location.create(-30,  20);
            y  = Location.create(  0,   0);
        }

        Port[] ps = new Port[ hasExtra ? 5 : 4 ];
        ps[idxA(hasExtra)] = new Port(a.getX(),  a.getY(),  Port.INPUT,  w);
        ps[idxB(hasExtra)] = new Port(b.getX(),  b.getY(),  Port.INPUT,  BitWidth.create(bMain));
        if (hasExtra) {
            ps[idxBX(true)]   = new Port(bx.getX(), bx.getY(), Port.INPUT, BitWidth.create(bExtra));
        }
        ps[idxS(hasExtra)] = new Port(s.getX(),  s.getY(),  Port.INPUT,  sw);
        ps[idxY(hasExtra)] = new Port(y.getX(),  y.getY(),  Port.OUTPUT, w);

        ps[idxA(hasExtra)].setToolTip(Strings.getter("pmuxATip"));
        ps[idxB(hasExtra)].setToolTip(Strings.getter("pmuxBTip"));
        if (hasExtra) ps[idxBX(true)].setToolTip(Strings.getter("pmuxBExtraTip"));
        ps[idxS(hasExtra)].setToolTip(Strings.getter("pmuxSelTip"));
        ps[idxY(hasExtra)].setToolTip(Strings.getter("pmuxYTip"));

        instance.setPorts(ps);
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        Direction facing = painter.getAttributeValue(StdAttr.FACING);
        Plexers.drawTrapezoid(painter.getGraphics(), painter.getBounds(), facing, 10);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Bounds bds = painter.getBounds();
        Direction facing = painter.getAttributeValue(StdAttr.FACING);

        boolean extra = hasExtra(painter);
        int sIdx = idxS(extra);
        Location selLoc = painter.getInstance().getPortLocation(sIdx);

        // stub del selector
        GraphicsUtil.switchToWidth(g, 3);
        if (painter.getShowState()) g.setColor(painter.getPort(sIdx).getColor());
        boolean vertical = (facing != Direction.NORTH && facing != SOUTH);
        int dx = vertical ? 0 : -1;
        int dy = vertical ? (facing == EAST || facing == WEST ? +1 : 0) : 0;
        g.drawLine(selLoc.getX() - 2 * dx, selLoc.getY() - 2 * dy, selLoc.getX(), selLoc.getY());
        GraphicsUtil.switchToWidth(g, 1);

        // círculo adentro
        drawSelectCircleInside(g, bds, selLoc);

        // cuerpo
        g.setColor(Color.BLACK);
        Plexers.drawTrapezoid(g, bds, facing, 10);
        GraphicsUtil.drawCenteredText(g, "PMUX",
                bds.getX() + bds.getWidth() / 2,
                bds.getY() + bds.getHeight() / 2);

        painter.drawPorts();

        int xA;
        int yA;
        int halign;

        if (facing == Direction.WEST) {
            xA = bds.getX() + bds.getWidth() - 3;
            yA = bds.getY() + 15;
            halign = GraphicsUtil.H_RIGHT;
        } else if (facing == Direction.NORTH) {
            xA = bds.getX() + 10;
            yA = bds.getY() + bds.getHeight() - 2;
            halign = GraphicsUtil.H_CENTER;
        } else if (facing == Direction.SOUTH) {
            xA = bds.getX() + 10;
            yA = bds.getY() + 12;
            halign = GraphicsUtil.H_CENTER;
        } else { // EAST
            xA = bds.getX() + 3;
            yA = bds.getY() + 15;
            halign = GraphicsUtil.H_LEFT;
        }

        g.setColor(Color.GRAY);
        GraphicsUtil.drawText(g, "0", xA, yA, halign, GraphicsUtil.V_BASELINE);
    }

    private static int bWidth(BitWidth w, BitWidth sw) {
        int W  = w.getWidth();
        int SW = sw.getWidth();
        int TOTAL  = W * SW;
        return Math.min(TOTAL, MAX_AGG);
    }

    private static boolean hasExtra(InstancePainter painter) {
        BitWidth w  = painter.getAttributeValue(StdAttr.WIDTH);
        BitWidth sw = painter.getAttributeValue(ATTR_SWIDTH);
        int cap = bWidth(w, sw);
        return cap > MAX_BUS; // hay B_X si cap > 32
    }

    private static void drawSelectCircleInside(Graphics g, Bounds bds, Location sel) {
        int locDelta = Math.max(bds.getHeight(), bds.getWidth()) <= 50 ? 8 : 6;
        int midX = bds.getX() + bds.getWidth()  / 2;
        int midY = bds.getY() + bds.getHeight() / 2;

        int dx = 0, dy = 0;

        if (Math.abs(sel.getX() - midX) > Math.abs(sel.getY() - midY)) {
            dx = (sel.getX() < midX) ? +locDelta : -locDelta;
        } else {
            dy = (sel.getY() < midY) ? +locDelta : -locDelta;
        }
        int cx = sel.getX() + dx, cy = sel.getY() + dy;
        g.setColor(Color.LIGHT_GRAY);
        g.fillOval(cx - 3, cy - 3, 6, 6);
    }

    @Override
    public void propagate(InstanceState state) {
        BitWidth w  = state.getAttributeValue(StdAttr.WIDTH);
        BitWidth sw = state.getAttributeValue(ATTR_SWIDTH);

        // Cap físico
        int cap      = bWidth(w, sw);
        int bMainLen = Math.min(cap, MAX_BUS);
        int bExtraLen= Math.max(0, cap - bMainLen);
        boolean extra = bExtraLen > 0;

        Value a  = state.getPort(idxA(extra));
        Value b0 = state.getPort(idxB(extra));
        Value bx = extra ? state.getPort(idxBX(true)) : Value.createKnown(BitWidth.create(0), 0);
        Value s  = state.getPort(idxS(extra));

        Value out = computeYWithSplitBWrap(w, sw, a, b0, bMainLen, bx, bExtraLen, s);
        state.setPort(idxY(extra), out, Math.max(1, w.getWidth()));
    }

    /** Núcleo con:
     *  - prioridad y X-prop como Yosys
     *  - B dividido en dos puertos, cap a 64 bits
     *  - wrap por slice si k excede slices físicos disponibles (64/W).
     */
    static Value computeYWithSplitBWrap(BitWidth width, BitWidth swidth,
                                        Value a,
                                        Value bMain, int bMainLen,
                                        Value bExtra, int bExtraLen,
                                        Value s) {
        int W  = width.getWidth();

        // capacidad física
        int capLen = bMainLen + bExtraLen;
        int physSlices = Math.max(1, capLen / Math.max(1, W)); // nº de slices disponibles físicamente

        // Errores en entradas
        if (a.isErrorValue() || bMain.isErrorValue() || bExtra.isErrorValue() || s.isErrorValue())
            return Value.createError(width);

        // Busca el primer '1' en S; si no hay, decide entre A/UNKNOWN
        Value[] sBits = s.getAll(); // SW bits, LSB en índice 0
        int k = -1;
        boolean anyUnknown = false;
        for (int i = 0; i < sBits.length; i++) {
            Value bi = sBits[i];
            if (bi == Value.TRUE) { k = i; break; }
            if (bi == Value.UNKNOWN) anyUnknown = true;
        }

        if (k < 0) {
            // No hay ningún '1' en S
            if (anyUnknown) return Value.createUnknown(width); // no podemos decidir
            return fitToWidth(a, width);                       // S == 0...0 → Y = A
        }

        // Elegimos el primer '1' encontrado con prioridad
        int kWrapped = k % physSlices;

        // Extraer slice kWrapped desde B||B_X (conservando UNKNOWN/ERROR bit a bit)
        Value[] bm = bMain.getAll();
        Value[] bx = bExtra.getAll();

        Value[] yy = new Value[W];
        int base = kWrapped * W;
        for (int i = 0; i < W; i++) {
            int bitIndex = base + i;
            if (bitIndex >= capLen) {
                // por seguridad si capLen no es múltiplo exacto de W: wrap por bit
                bitIndex = bitIndex % Math.max(1, capLen);
            }
            if (bitIndex < bMainLen) {
                yy[i] = bm[bitIndex];
            } else {
                int j = bitIndex - bMainLen;
                yy[i] = (j >= 0 && j < bx.length) ? bx[j] : Value.FALSE;
            }
        }
        return Value.create(yy);
    }

    private static Value fitToWidth(Value v, BitWidth w) {
        if (v.getWidth() == w.getWidth()) return v;
        if (!v.isFullyDefined()) {
            Value[] src = v.getAll();
            Value[] dst = new Value[w.getWidth()];
            int n = Math.min(src.length, dst.length);
            System.arraycopy(src, 0, dst, 0, n);
            for (int i = n; i < dst.length; i++) dst[i] = Value.FALSE;
            return Value.create(dst);
        }
        int ww = w.getWidth();
        int mask = (ww >= 31) ? -1 : ((1 << ww) - 1);
        return Value.createKnown(w, v.toIntValue() & mask);
    }
}

