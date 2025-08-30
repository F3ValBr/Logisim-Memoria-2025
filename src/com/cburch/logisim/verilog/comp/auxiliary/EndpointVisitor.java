package com.cburch.logisim.verilog.comp.auxiliary;

/**
 * Visitor interface for endpoints of a net.
 * Used in NetTraversal to process each endpoint.
 */
public interface EndpointVisitor {
    void topPort(int portIdx, int bitIdx);
    void cellBit(int cellIdx, int bitIdx);
}

