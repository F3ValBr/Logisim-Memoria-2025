package com.cburch.logisim.verilog.layout.endpoints;

public sealed interface EndpointRef permits TopPortRef, CellPortRef {
    boolean isTop();
}
