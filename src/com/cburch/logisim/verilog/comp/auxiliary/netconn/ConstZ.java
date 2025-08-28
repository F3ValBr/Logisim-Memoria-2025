package com.cburch.logisim.verilog.comp.auxiliary.netconn;

public final class ConstZ implements BitRef {
    private static final ConstZ INSTANCE = new ConstZ();

    private ConstZ() {
        // Private constructor to prevent instantiation
    }

    public static ConstZ getInstance() { return INSTANCE; }

    @Override
    public String toString() { return "ConstZ"; }
}
