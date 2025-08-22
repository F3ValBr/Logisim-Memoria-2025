package com.cburch.logisim.verilog.comp.specs;

import java.util.Map;

public final class CommonOpAttribs extends GenericCellAttribs {
    public CommonOpAttribs(Map<String,?> raw) { super(raw); }
    public String src() { return getString("src", ""); }
    public boolean keep() { return getBool("keep", false); }
    public boolean dontTouch() { return getBool("dont_touch", false); }
    public boolean moduleNotDerived() { return getBool("module_not_derived", false); }
}
