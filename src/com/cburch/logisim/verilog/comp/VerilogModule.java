package com.cburch.logisim.verilog.comp;

import com.cburch.logisim.instance.Port;

import java.util.List;

public interface VerilogModule {
    String name();
    List<Port> ports();
    List<VerilogCell> cells();

    void addCell(VerilogCell cell);
    String getPortName(Port port);
}