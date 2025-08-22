package com.cburch.logisim.verilog.comp.aux.netconn;

public final class NetBit implements BitRef {
    private final int netId;

    public NetBit(int netId) {
        this.netId = netId;
    }

    public int getNetId() {
        return netId;
    }
}
