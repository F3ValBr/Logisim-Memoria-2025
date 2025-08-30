package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;

public class DLatchParams extends RegisterOpParams {
    public DLatchParams(java.util.Map<String, ?> raw) {
        super(raw);
        validate();
    }

    public boolean enPolarity() { return bit("EN_POLARITY", true); } // si Yosys lo expone; si no, asume 1

    @Override protected void validate() {
        require(width() > 0, "WIDTH must be > 0");
    }
}
