package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memwriteparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOpParams;

import java.util.Map;

public abstract class AbstractMemWRParams extends MemoryOpParams {
    public AbstractMemWRParams(Map<String, ?> raw) {
        super(raw);
    }

    // ===== Sincronismo / reloj =====
    @Override
    public boolean clkEnable() { return getBool("CLK_ENABLE", true); }
    public boolean clkPolarity() { return getBool("CLK_POLARITY", true); }
}
