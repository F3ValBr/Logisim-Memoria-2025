package com.cburch.logisim.std.riscv;


import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.*;

public class RV32IMMicro extends Processor{
    public RV32IMMicro(){
        super("RV32IM_Micro","processorRV32IMMicro","Rom.gif");
        setOffsetBounds(Bounds.create(-40, -40, 80, 80));
    }

    @Override
    void configurePorts(Instance instance) {
        Port[] ps = new Port[MEM_INPUTS];
        ps[DATA] = new Port(40, 0, Port.INOUT, b32);
        ps[ADDR] = new Port(-40, 0, Port.OUTPUT, b32);
        ps[BE] = new Port(-40, 20, Port.OUTPUT, b4);
        ps[RD] = new Port(-10, 40, Port.OUTPUT, 1);
        ps[WR] = new Port(10, 40, Port.OUTPUT, 1);
        ps[CLK] = new Port(30, 40, Port.INPUT, 1);
        ps[INTR] = new Port(0, -40, Port.INPUT, 1);
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
        GraphicsUtil.drawCenteredText(g, "RV32IM",
                bds.getX() + bds.getWidth() / 2,
                bds.getY() + bds.getHeight()/4);
        if(painter.getAttributeValue(ENABLE_MEMORY)==ENABLE){
            String text=memorySizeToString(painter.getAttributeValue(ADDR_ATTR).getWidth()+2);
            GraphicsUtil.drawCenteredText(g, text, bds.getX() + bds.getWidth()
                    / 2, bds.getY() + bds.getHeight()*3/ 4-10);
        }
    }
}
