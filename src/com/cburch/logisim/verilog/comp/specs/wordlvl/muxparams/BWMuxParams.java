package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

public final class BWMuxParams extends MuxOpParams {
    public BWMuxParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    public int sWidth() { return getInt("S_WIDTH", 0); }

    private void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(sWidth() > 0, "S_WIDTH must be > 0");
    }
}
