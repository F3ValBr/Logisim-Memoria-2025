package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memwriteparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOpParams;

import java.util.Map;

public abstract class AbstractMemWRParams extends MemoryOpParams {
    public AbstractMemWRParams(Map<String, ?> raw) {
        super(raw);
    }

    // ===== Escalares obligatorios =====
    public String memId() { return getString("MEMID", ""); }
    public int abits() { return getInt("ABITS", 0); }
    public int width() { return getInt("WIDTH", 0); }

    // ===== Sincronismo / reloj =====
    public boolean clkEnable() { return getBool("CLK_ENABLE", true); }
    public boolean clkPolarity() { return getBool("CLK_POLARITY", true); }
}
