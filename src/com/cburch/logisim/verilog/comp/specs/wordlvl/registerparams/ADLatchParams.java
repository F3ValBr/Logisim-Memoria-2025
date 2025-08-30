package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;

import java.util.Map;

public class ADLatchParams extends RegisterOpParams {
    public ADLatchParams(Map<String, ?> raw){
        super(raw);
        validate();
    }

    public boolean enPolarity(){ return bit("EN_POLARITY", true); }
    public boolean arstPolarity(){ return bit("ARST_POLARITY", true); }
    public long   arstValue(){ return getLong("ARST_VALUE", 0L); }

    @Override protected void validate() {
        require(width()>0, "WIDTH must be > 0");
    }
}
