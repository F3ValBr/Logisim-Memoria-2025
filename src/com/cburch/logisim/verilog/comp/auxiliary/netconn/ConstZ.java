package com.cburch.logisim.verilog.comp.auxiliary.netconn;

/**
 * A singleton class representing a constant high-impedance (Z) logic bit.
 * Implements the BitRef interface.
 */
public final class ConstZ implements BitRef {
    private static final ConstZ INSTANCE = new ConstZ();

    private ConstZ() {
        // Private constructor to prevent instantiation
    }

    public static ConstZ getInstance() { return INSTANCE; }

    @Override
    public String toString() { return "ConstZ"; }
}
