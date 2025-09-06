package com.cburch.logisim.verilog.layout.endpoints;

/** Un extremo que está en un puerto de celda interna. */
public record CellPortRef(int cellIdx, int bitIdx) implements EndpointRef {
    @Override public boolean isTop() { return false; }
}
