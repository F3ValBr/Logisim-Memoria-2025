package com.cburch.logisim.verilog.comp;

import com.cburch.logisim.verilog.comp.aux.CellType;
import com.cburch.logisim.verilog.comp.aux.PortEndpoint;
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
}
