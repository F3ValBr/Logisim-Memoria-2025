package com.cburch.logisim.verilog.layout.endpoints;

/** Un extremo que está en un puerto de módulo (top-level). */
public record TopPortRef(int topPortIdx, int bitIdx) implements EndpointRef {
    @Override public boolean isTop() { return true; }
}
