package com.cburch.logisim.verilog.comp.specs.gatelvl;

import com.cburch.logisim.verilog.comp.specs.GenericCellParams;

import java.util.Map;

public final class GateParams extends GenericCellParams {
    private final String gate; // "AND","MUX",...
    public GateParams(String gate, Map<String,?> raw) { super(raw); this.gate = gate; }
    public String gate() { return gate; }
}
