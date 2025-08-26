package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.dffeparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.DFFParams;

import java.util.Map;

public final class DFFEParams extends DFFParams implements EnableCapableParams {
    public DFFEParams(Map<String, ?> raw){ super(raw); validate(); }

    @Override public boolean enPolarity(){ return bit("EN_POLARITY", true); } // si Yosys lo expone; si no, asume 1
}

