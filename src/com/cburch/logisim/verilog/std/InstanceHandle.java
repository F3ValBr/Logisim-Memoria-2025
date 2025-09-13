package com.cburch.logisim.verilog.std;

import com.cburch.logisim.comp.Component;

public final class InstanceHandle {
    public final Component component;
    public final PinLocator pins;
    public InstanceHandle(Component c, PinLocator p){ component=c; pins=p; }
}
