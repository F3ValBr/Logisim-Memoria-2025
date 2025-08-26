package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;

import java.util.Map;

public class SRTypeParams extends RegisterOpParams {
    public SRTypeParams(Map<String, ?> raw){ super(raw); validate(); }

    public boolean setPolarity(){ return bit("SET_POLARITY", true); }
    public boolean rstPolarity(){ return bit("CLR_POLARITY", true); }

    @Override protected void validate() {
        require(width()>0, "WIDTH must be > 0");
    }
}

