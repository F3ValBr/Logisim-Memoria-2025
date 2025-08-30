package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memwriteparams;

import java.util.BitSet;
import java.util.Map;

/**
 * Parameters for memory write operations (MEMWR).
 * <p>
 * See AbstractMemWRParams for base parameters.
 */
public class MemWRParams extends AbstractMemWRParams {
    public MemWRParams(Map<String, ?> raw) { super(raw); validate(); }

    public BitSet priority() { return getMask("PRIORITY"); }

    @Override
    protected void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(abits() > 0, "ABITS must be > 0");
        require(!memId().isEmpty(), "MEMID must be non-empty");
    }
}
