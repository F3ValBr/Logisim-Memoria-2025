package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;

public class TimerMem implements InstanceData, Cloneable{
    public long timer=System.currentTimeMillis();
    public Value last=Value.FALSE;
    public TimerMem(){}

    @Override
    public TimerMem clone() {
        try {
            TimerMem ret= (TimerMem) super.clone();
            ret.last=last;
            ret.timer=timer;
            return ret;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
