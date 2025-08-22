package com.cburch.logisim.verilog.comp.auxiliary.netconn;

public enum Direction {
    INPUT("input"),
    OUTPUT("output"),
    INOUT("inout"),
    UNKNOWN("unknown");

    private final String dirValue;

    Direction(String dirValue) {
        this.dirValue = dirValue;
    }

    public String getJsonValue() {
        return dirValue;
    }

    public static Direction fromJson(String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }
        for (Direction dir : values()) {
            if (dir.dirValue.equalsIgnoreCase(value)) {
                return dir;
            }
        }
        throw new IllegalArgumentException("Unknown direction: " + value);
    }
}