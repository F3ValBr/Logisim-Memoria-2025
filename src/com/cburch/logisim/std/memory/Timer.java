package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.*;

public class Timer extends InstanceFactory {
    BitWidth b32=BitWidth.create(32);
    public Timer() {
        super("Timer");
        setOffsetBounds(Bounds.create(-30, -20, 30, 40));
        Port[] ps = new Port[2];
        ps[0] = new Port(  0,   0, Port.INPUT, 1);
        ps[1]  = new Port(-30,   0, Port.OUTPUT, 32);
        setPorts(ps);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        painter.drawBounds();
    }

    @Override
    public void propagate(InstanceState state) {
        TimerMem data= (TimerMem) state.getData();
        if(data==null){
            data = new TimerMem();
            state.setData(data);
        }
        Value select=state.getPort(0);
        if(select==Value.TRUE){
            if(data.last==Value.FALSE){
                long newTime=System.currentTimeMillis();
                int time=(int) ((newTime-data.timer));
                state.setPort(1,Value.createKnown(b32,time),0);
                data.timer=newTime;
            }
        }
        data.last=select;
    }
}
