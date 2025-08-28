package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memreadparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOpParams;

import java.util.Map;

public abstract class AbstractMemRDParams extends MemoryOpParams {
    public AbstractMemRDParams(Map<String, ?> raw) { super(raw); }

    // ===== Sincronismo / reloj / enable =====
    @Override
    public boolean clkEnable() { return getBool("CLK_ENABLE", false); }
    public boolean clkPolarity() { return getBool("CLK_POLARITY", true); }
}
