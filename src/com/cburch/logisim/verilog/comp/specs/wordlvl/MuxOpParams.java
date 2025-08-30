package com.cburch.logisim.verilog.comp.specs.wordlvl;

import com.cburch.logisim.verilog.comp.specs.GenericCellParams;

import java.util.Map;

public abstract class MuxOpParams extends GenericCellParams {
    protected MuxOpParams(Map<String, ?> raw) {
        super(raw);
    }

    public int width() { return getInt("WIDTH", 0); }

    protected void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(getClass().getSimpleName() + ": " + message);
    }
}
