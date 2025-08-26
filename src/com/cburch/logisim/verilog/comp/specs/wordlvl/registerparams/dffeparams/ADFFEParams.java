package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.dffeparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.ADFFParams;

import java.util.Map;

public final class ADFFEParams extends ADFFParams implements EnableCapableParams {
    public ADFFEParams(Map<String, ?> raw){ super(raw); validate(); }

    @Override public boolean enPolarity(){ return bit("EN_POLARITY", true); }
}
