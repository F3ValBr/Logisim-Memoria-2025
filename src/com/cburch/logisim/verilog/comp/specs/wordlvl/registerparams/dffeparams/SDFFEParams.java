package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.dffeparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.SDFFParams;

import java.util.Map;

public final class SDFFEParams extends SDFFParams implements EnableCapableParams {
    public SDFFEParams(Map<String, ?> raw){ super(raw); validate(); }

    @Override public boolean enPolarity(){ return bit("EN_POLARITY", true); }
}
