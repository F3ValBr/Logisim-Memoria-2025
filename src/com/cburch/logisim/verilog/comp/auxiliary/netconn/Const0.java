package com.cburch.logisim.verilog.comp.auxiliary.netconn;

public final class Const0 implements BitRef {
    private static final Const0 INSTANCE = new Const0();

    private Const0() {
        // Private constructor to prevent instantiation
    }

    public static Const0 getInstance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "Const0";
    }
}
