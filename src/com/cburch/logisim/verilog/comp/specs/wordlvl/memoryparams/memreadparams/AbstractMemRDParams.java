package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memreadparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOpParams;

import java.util.Map;

public abstract class AbstractMemRDParams extends MemoryOpParams {
    public AbstractMemRDParams(Map<String, ?> raw) { super(raw); }

    // ---- escalares base ----
    public String memId() { return getString("MEMID", ""); }  // identificador de memoria
    public int    abits() { return getInt("ABITS", 0); }      // bits de dirección
    public int    width() { return getInt("WIDTH", 0); }      // bits por palabra

    // ===== Sincronismo / reloj / enable =====
    public boolean clkEnable() { return getBool("CLK_ENABLE", false); }
    public boolean clkPolarity() { return getBool("CLK_POLARITY", true); }
}
