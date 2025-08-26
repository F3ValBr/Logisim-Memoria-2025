package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;

import java.util.Map;

public class SDFFParams extends RegisterOpParams {
    public SDFFParams(Map<String, ?> raw){ super(raw); validate(); }

    public boolean clkPolarity(){ return bit("CLK_POLARITY", true); }
    public boolean srstPolarity(){ return bit("SRST_POLARITY", true); }
    public long   srstValue(){ return getLong("SRST_VALUE", 0L); }

    @Override protected void validate() {
        require(width()>0, "WIDTH must be > 0");
    }
}

