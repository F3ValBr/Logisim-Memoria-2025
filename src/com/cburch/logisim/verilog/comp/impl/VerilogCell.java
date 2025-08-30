package com.cburch.logisim.verilog.comp.impl;

import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.PortEndpoint;
import com.cburch.logisim.verilog.comp.specs.CellAttribs;
import com.cburch.logisim.verilog.comp.specs.CellParams;

import java.util.List;

public interface VerilogCell {
    String name();
    CellType type();
    CellParams params();
    CellAttribs attribs();
    List<PortEndpoint> endpoints();

    int portWidth(String portName);
    List<String> getPortNames();
    void addPortEndpoint(PortEndpoint endpoint);
    String typeId();
    String kind();
    String level();
}
