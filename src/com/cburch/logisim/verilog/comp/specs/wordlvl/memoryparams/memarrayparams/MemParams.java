package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memarrayparams;

import java.util.Map;

/**
 * Parameters for memory arrays (MEM).
 * <p>
 * See AbstractMemParams for base parameters.
 */
public class MemParams extends AbstractMemParams {
    public MemParams(Map<String, ?> raw) { super(raw); validate(); }

    // ---- ports (global values for all RD/WR) ----
    public boolean rdClkEnable() { return getBool("RD_CLK_ENABLE",  true); }
    public boolean rdClkPolarity(){return getBool("RD_CLK_POLARITY",true); }
    public boolean rdTransparent(){return getBool("RD_TRANSPARENT", true); }

    public boolean wrClkEnable() { return getBool("WR_CLK_ENABLE",  true); }
    public boolean wrClkPolarity(){return getBool("WR_CLK_POLARITY",true); }

    // ---- validations ----
    @Override
    protected void validate() {
        require(width()  > 0, "WIDTH must be > 0");
        require(size()   > 0, "SIZE must be > 0");
        require(abits()  > 0, "ABITS must be > 0");
        long cap = 1L << Math.min(abits(), 62);
        require(size() <= cap, "SIZE exceeds address capacity (ABITS)");
        require(rdPorts() >= 0, "RD_PORTS must be >= 0");
        require(wrPorts() >= 0, "WR_PORTS must be >= 0");
        // INIT: if present, must be valid and match size*width
        String s = initRaw().trim();
        if (!s.isEmpty() && !isInitUndef()) {
            // only parse if not empty or undefined
            parseBits(s, size() * width()); // sanity check
        }
    }
}

