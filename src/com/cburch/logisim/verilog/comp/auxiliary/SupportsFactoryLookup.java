package com.cburch.logisim.verilog.comp.auxiliary;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;

public interface SupportsFactoryLookup {
    ComponentFactory peekFactory(Project proj, VerilogCell cell);
}
