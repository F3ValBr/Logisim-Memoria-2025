package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

/** Params para $tribuf (tristate). */
public final class TribufParams extends MuxOpParams {
    public TribufParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    private void validate() {
        int w = width();
        require(w > 0, "WIDTH must be > 0");
    }

    public int enWidth() { return 1; }
}

