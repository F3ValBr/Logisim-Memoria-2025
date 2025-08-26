package com.cburch.logisim.verilog.comp.specs.wordlvl;

import com.cburch.logisim.verilog.comp.specs.GenericCellParams;

import java.util.Map;

public abstract class RegisterOpParams extends GenericCellParams {
    protected RegisterOpParams(Map<String, ?> raw) { super(raw); }

    // Presentes en casi todos
    public int width() { return getInt("WIDTH", 0); }

    // 0/1 en Yosys → boolean
    protected boolean bit(String key, boolean def){ return getBool(key, def); }

    protected void require(boolean cond, String msg){
        if(!cond) throw new IllegalArgumentException(getClass().getSimpleName()+": "+msg);
    }

    /** Validaciones específicas de cada subtipo. */
    protected abstract void validate();
}
