package com.cburch.logisim.std.riscv;

import com.cburch.logisim.data.*;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;
import java.util.List;

public class RV32IM extends Processor{
    static final int SIZE=150;
    static final AttributeOption SHOW_REGISTER
            = new AttributeOption("showRegister", Strings.getter("processorShowRegister"));
    static final AttributeOption HIDE_REGISTER
            = new AttributeOption("hideRegister", Strings.getter("processorHideRegister"));
    static final Attribute<AttributeOption> DISPLAY_REGISTER
            = Attributes.forOption("displayRegister",Strings.getter("processorDisplayRegister"),
            new AttributeOption[] { SHOW_REGISTER,HIDE_REGISTER});
    public RV32IM() {
        super("RV32IM","processorRV32IM","Rom.gif");
        setOffsetBounds(Bounds.create(-SIZE, -SIZE, 2*SIZE, 2*SIZE));
    }
    @Override
    public AttributeSet createAttributeSet() {
        java.util.List<Attribute<?>> add = List.of(DISPLAY_REGISTER);
        return new ProcessorAttributes(add);
    }
    @Override
    void configurePorts(Instance instance) {
        Port[] ps = new Port[MEM_INPUTS];
        ps[DATA] = new Port(SIZE, 40, Port.INOUT, b32);
        ps[ADDR] = new Port(-SIZE, 40, Port.OUTPUT, b32);
        ps[BE] = new Port(-SIZE, 100, Port.OUTPUT, b4);
        ps[RD] = new Port(-100, SIZE, Port.OUTPUT, 1);
        ps[WR] = new Port(-80, SIZE, Port.OUTPUT, 1);
        ps[CLK] = new Port(0, SIZE, Port.INPUT, 1);
        ps[INTR] = new Port(0, -SIZE, Port.INPUT, 1);
        configureTipPorts(ps);
        instance.setPorts(ps);
    }
    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Bounds bds = painter.getBounds();
        painter.drawBounds();
        painter.drawPort(DATA, Strings.get("DATA"), Direction.WEST);
        painter.drawPort(ADDR, Strings.get("ADDR"), Direction.EAST);
        painter.drawPort(BE, Strings.get("BE"), Direction.EAST);
        painter.drawPort(RD, Strings.get("RD"), Direction.SOUTH);
        painter.drawPort(WR, Strings.get("WR"), Direction.SOUTH);
        painter.drawPort(INTR, Strings.get("INTR"), Direction.NORTH);
        painter.drawClock(CLK, Direction.NORTH);
        GraphicsUtil.drawCenteredText(g, "RV32IM", bds.getX() + bds.getWidth()
                / 2, bds.getY() + bds.getHeight()*3/ 4);
        ProcessorData data=(ProcessorData) painter.getData();
        if(painter.getAttributeValue(ENABLE_MEMORY)==ENABLE){
            String text=memorySizeToString(painter.getAttributeValue(ADDR_ATTR).getWidth()+2);

            GraphicsUtil.drawCenteredText(g, text, bds.getX() + bds.getWidth()
                    / 2, bds.getY() + bds.getHeight()*3/ 4-10);
        }

        if(painter.getAttributeValue(DISPLAY_REGISTER)==SHOW_REGISTER && data!=null){
            data.paint(g,bds.getX(), bds.getY());
        }
}
}
