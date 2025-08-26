package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

public final class MuxParams extends MuxOpParams {
    public MuxParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    private void validate() {
        int w = width();
        require(w > 0, "WIDTH debe ser positivo");
    }

    public int sWidth() { return 1; }
}
