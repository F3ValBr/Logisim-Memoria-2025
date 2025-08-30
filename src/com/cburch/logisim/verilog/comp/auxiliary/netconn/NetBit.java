package com.cburch.logisim.verilog.comp.auxiliary.netconn;

/**
 * Represents a reference to a specific bit in a net (wire or bus).
 * Implements the BitRef interface.
 */
public final class NetBit implements BitRef {
    private final int netId;

    public NetBit(int netId) {
        this.netId = netId;
    }

    public int getNetId() {
        return netId;
    }
}
