package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memwriteparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOpParams;

import java.util.Map;

/**
 * Abstract base class for write port memory operation parameters.
 * Handles clock synchronization and polarity settings.
 */
public abstract class AbstractMemWRParams extends MemoryOpParams {
    public AbstractMemWRParams(Map<String, ?> raw) {
        super(raw);
    }

    // ===== Synchronism / clock =====
    /**
     * Returns true if the clock is enabled for this write port.
     */
    @Override
    public boolean clkEnable() { return getBool("CLK_ENABLE", true); }

    /**
     * Returns the polarity of the clock signal for this write port.
     * True usually means rising edge, false means falling edge.
     */
    public boolean clkPolarity() { return getBool("CLK_POLARITY", true); }
}
