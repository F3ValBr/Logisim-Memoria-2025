package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;

public class ERom extends Rom {
    static final int WR = MEM_INPUTS;
    public ERom() {
        super("ERom","ERomComponent",2,"Rom.gif");
    }
    @Override
    void configurePorts(Instance instance) {
        Port[] ps = new Port[MEM_INPUTS+1];
        configureStandardPorts(instance, ps);
        ps[WR]   = new Port( -50, 40, Port.INPUT, 1);
        ps[WR].setToolTip(Strings.getter("WRTip"));
        instance.setPorts(ps);
    }
    @Override
    public void paintInstance(InstancePainter painter){
        super.paintInstance(painter);
        painter.drawPort(WR, Strings.get("ramWELabel"), Direction.SOUTH);
    }
    @Override
    MemContents getMemContents(Instance instance) {
        return instance.getAttributeValue(CONTENTS_ATTR).clone();
    }
    @Override
    public void propagate(InstanceState state) {
        if (state.getPort(WR).equals(Value.TRUE)
                & state.getPort(CS).equals(Value.TRUE)) {
            Value addrValue = state.getPort(ADDR);
            Value dataValue = state.getPort(DATA);
            MemState myState = getState(state);
            int addr = addrValue.toIntValue();
            if (!dataValue.isFullyDefined()
                    || !addrValue.isFullyDefined()
                    || addr < 0)
                return;
            if (addr != myState.getCurrent()) {
                myState.setCurrent(addr);
                myState.scrollToShow(addr);
            }
            myState.getContents().set(addr,dataValue.toIntValue());
        }
        else{
            super.propagate(state);
        }
    }
}
