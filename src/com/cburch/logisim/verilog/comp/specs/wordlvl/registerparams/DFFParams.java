package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;

import java.util.Map;

public class DFFParams extends RegisterOpParams {
    public DFFParams(Map<String, ?> raw){
        super(raw);
        validate();
    }

    public boolean clkPolarity(){ return bit("CLK_POLARITY", true); } // 1=pos, 0=neg

    @Override protected void validate(){
        require(width()>0, "WIDTH must be > 0");
    }
}
