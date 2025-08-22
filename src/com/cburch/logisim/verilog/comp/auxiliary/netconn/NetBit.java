package com.cburch.logisim.verilog.comp.auxiliary.netconn;

public final class NetBit implements BitRef {
    private final int netId;

    public NetBit(int netId) {
        this.netId = netId;
    }

    public int getNetId() {
        return netId;
    }
}
