package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memwriteparams;

import java.util.BitSet;
import java.util.Map;

/**
 * Parameters for a write port in the memory operation (version 2).
 * Handles port identity, priority mask, and validation logic.
 */
public class MemWRV2Params extends AbstractMemWRParams {
    public MemWRV2Params(Map<String, ?> raw) { super(raw); validate(); }

    // ===== Identity and priority among write ports =====
    public int portId() { return getInt("PORTID", -1); }
    public BitSet priorityMask() { return getMask("PRIORITY_MASK"); }

    @Override
    protected void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(abits() > 0, "ABITS must be > 0");
        require(portId() >= 0, "PORTID must be >= 0");
        require(!memId().isEmpty(), "MEMID must be non-empty");

        // PRIORITY_MASK: by specification, can only have priority over ports with lower PORTID.
        // If bits above portId are set, mark as invalid
        // TODO: check this
        BitSet pm = priorityMask();
        for (int i = portId(); i < pm.length(); i++) {
            if (pm.get(i)) {
                throw new IllegalArgumentException(
                        "Invalid PRIORITY_MASK: port " + portId() +
                                " cannot have priority over portId >= " + portId() + " (bit " + i + " set)"
                );
            }
        }
    }
}
