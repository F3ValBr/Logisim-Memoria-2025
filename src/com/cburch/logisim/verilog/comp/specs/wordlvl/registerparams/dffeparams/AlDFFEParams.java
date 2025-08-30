package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.dffeparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.AlDFFParams;

import java.util.Map;

public final class AlDFFEParams extends AlDFFParams implements EnableCapableParams {
    public AlDFFEParams(Map<String, ?> raw){ super(raw); validate(); }

    @Override public boolean enPolarity(){ return bit("EN_POLARITY", true); }
}
