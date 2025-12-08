package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;

/**
 * Estructura de estado del Timer.
 * Debe implementar InstanceData y proveer clone().
 */
public class TimerData implements InstanceData {
    long lastRealTime = 0L;

    long virtualTicks = 0L;
    long lastVirtualAtTrigger = 0L;

    Value lastSelect = Value.FALSE;
    Value lastClock  = Value.FALSE;

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (Exception e) {
            TimerData c = new TimerData();
            c.lastRealTime = this.lastRealTime;
            c.virtualTicks = this.virtualTicks;
            c.lastVirtualAtTrigger = this.lastVirtualAtTrigger;
            c.lastSelect = this.lastSelect;
            c.lastClock = this.lastClock;
            return c;
        }
    }
}
