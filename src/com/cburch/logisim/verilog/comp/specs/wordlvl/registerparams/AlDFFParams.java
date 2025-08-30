package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;

import java.util.Map;

public class AlDFFParams extends RegisterOpParams {
    public AlDFFParams(Map<String, ?> raw){
        super(raw);
        validate();
    }

    public boolean clkPolarity(){ return bit("CLK_POLARITY", true); } // 1=pos, 0=neg
    public boolean aloadPolarity(){ return bit("ALOAD_POLARITY", true); } // 1=active high, 0=active low

    @Override protected void validate(){
        require(width()>0, "WIDTH must be > 0");
    }
}
