package com.cburch.logisim.verilog.comp.specs;

import java.util.Map;

public final class CommonOpAttribs extends GenericCellAttribs {
    /** Common attributes for operations (both binary and unary):
     * - "src": String with source location in RTL code (e.g. "factorial.sv:13.19-13.65")
     * - "keep": boolean, if true, Yosys will not optimize away this operation
     * - "dont_touch": boolean, if true, Yosys will not modify this operation
     * - "module_not_derived": boolean, if true, indicates this module was marked by Yosys as "not derived"
     */
    public CommonOpAttribs(Map<String,?> raw) { super(raw); }
    public String src() { return getString("src", ""); }
    public boolean keep() { return getBool("keep", false); }
    public boolean dontTouch() { return getBool("dont_touch", false); }
    public boolean moduleNotDerived() { return getBool("module_not_derived", false); }
}
