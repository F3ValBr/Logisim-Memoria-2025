package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.dffeparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.DFFSRParams;

import java.util.Map;

public final class DFFSREParams extends DFFSRParams implements EnableCapableParams {
    public DFFSREParams(Map<String, ?> raw){ super(raw); validate(); }

    @Override public boolean enPolarity(){ return bit("EN_POLARITY", true); }
}
