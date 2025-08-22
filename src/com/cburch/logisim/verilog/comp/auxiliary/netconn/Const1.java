package com.cburch.logisim.verilog.comp.auxiliary.netconn;

public final class Const1 implements BitRef {
    private static final Const1 INSTANCE = new Const1();

    private Const1() {
        // Private constructor to prevent instantiation
    }

    public static Const1 getInstance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "Const1";
    }
}
