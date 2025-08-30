package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.meminitparams;

import java.util.Map;

/**
 * Parameters for memory initialization (MEM_INIT).
 * <p>
 * See AbstractMemInitParams for base parameters.
 */
public class MemInitParams extends AbstractMemInitParams {
    public MemInitParams(Map<String, ?> raw) { super(raw); validate(); }
}
