package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams;

import java.util.Map;

public class DLatchSRParams extends DLatchParams {
    public DLatchSRParams(Map<String, ?> raw){
        super(raw);
        validate();
    }

    public boolean setPolarity(){ return bit("SET_POLARITY", true); }
    public boolean rstPolarity(){ return bit("CLR_POLARITY", true); }
}
