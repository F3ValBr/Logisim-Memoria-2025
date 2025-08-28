package com.cburch.logisim.verilog.comp.auxiliary.netconn;

public final class ConstX implements BitRef {
    private static final ConstX INSTANCE = new ConstX();

    private ConstX() {
        // Private constructor to prevent instantiation
    }

    public static ConstX getInstance() { return INSTANCE; }

    @Override
    public String toString() { return "ConstX"; }
}

