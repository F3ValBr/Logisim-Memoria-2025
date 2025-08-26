package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

public final class BMuxParams extends MuxOpParams {
    public BMuxParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    public int sWidth() { return getInt("S_WIDTH", 0); }

    /** Cantidad total de bits en A. */
    public int aTotalWidth() {
        return width() * sWidth();
    }

    private void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(sWidth() > 0, "S_WIDTH must be > 0");
    }
}
