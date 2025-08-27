package com.cburch.logisim.verilog.comp.specs.wordlvl;

import com.cburch.logisim.verilog.comp.specs.GenericCellParams;

import java.util.Map;

public abstract class MemoryOpParams extends GenericCellParams {
    protected MemoryOpParams(Map<String, ?> raw){ super(raw); }

    protected void require(boolean cond, String msg){
        if(!cond) throw new IllegalArgumentException(getClass().getSimpleName()+": "+msg);
    }
}

