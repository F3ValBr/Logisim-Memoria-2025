package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.dffeparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.SDFFParams;

import java.util.Map;

public final class SDFFCEParams extends SDFFParams implements EnableCapableParams {
    public SDFFCEParams(Map<String, ?> raw){ super(raw); validate(); }

    @Override public boolean enPolarity(){ return bit("EN_POLARITY", true); }
}
