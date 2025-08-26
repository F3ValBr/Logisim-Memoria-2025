package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;

import java.util.Map;

public class ADFFParams extends RegisterOpParams {
    public ADFFParams(Map<String, ?> raw){ super(raw); validate(); }

    public boolean clkPolarity(){ return bit("CLK_POLARITY", true); }
    public boolean arstPolarity(){ return bit("ARST_POLARITY", true); }
    public long   arstValue(){ return getLong("ARST_VALUE", 0L); } // si viene en binario, GenericCellParams lo parsea

    @Override protected void validate(){
        require(width() > 0, "WIDTH must be > 0");
        // opcional: que ARST_VALUE quepa en WIDTH (si te interesa)
        require(arstValue() >= 0, "ARST_VALUE must be non-negative");
    }
}

