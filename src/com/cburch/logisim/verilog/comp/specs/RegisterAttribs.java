package com.cburch.logisim.verilog.comp.specs;

import java.util.Map;

public final class RegisterAttribs extends GenericCellAttribs {
    public RegisterAttribs(Map<String,?> raw) { super(raw); }
    public boolean alwaysFF() { return getBool("always_ff", false); }
    public String src() { return getString("src", ""); }
}
