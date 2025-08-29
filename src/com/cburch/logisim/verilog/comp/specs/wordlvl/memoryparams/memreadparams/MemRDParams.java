package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memreadparams;

import java.util.BitSet;
import java.util.Map;

/**
 * Parameters for memory read (MEMRD).
 * <p>
 * See AbstractMemRDParams for base parameters.
 */
public class MemRDParams extends AbstractMemRDParams {
    public MemRDParams(Map<String, ?> raw) { super(raw); validate(); }

    public BitSet transparent() { return getMaskFlexible("TRANSPARENT"); }

    @Override
    protected void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(abits() > 0, "ABITS must be > 0");
        require(!memId().isEmpty(), "MEMID must be non-empty");

        // If asynchronous, CLK is not used; if synchronous, it may or may not be present depending on the netlist.
        // Here, we only validate parameter consistency:
        if (!clkEnable()) {
            // No additional checks; CLK may be connected as "x" in connections and is valid.
        }
    }

    /** Returns a mask (BitSet) without checking the exact length (it is trimmed to expected if you pass -1). */
    private BitSet getMaskFlexible(String key) {
        return getMask(key, -1, false);
    }
}
