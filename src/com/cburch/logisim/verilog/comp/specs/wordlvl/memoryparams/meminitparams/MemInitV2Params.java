package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.meminitparams;

import java.util.Map;

/**
 * Parameters for memory initialization (MEM_INIT_V2).
 * <p>
 * See AbstractMemInitParams for base parameters.
 */
public class MemInitV2Params extends AbstractMemInitParams {
    public MemInitV2Params(Map<String, ?> raw) { super(raw); validate(); }
}
