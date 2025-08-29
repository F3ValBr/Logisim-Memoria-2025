package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.meminitparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOpParams;

import java.util.Map;

/**
 * Base class for memory initialization parameters (MEM_INIT and MEM_INIT_V2).
 */
public abstract class AbstractMemInitParams extends MemoryOpParams {
    public AbstractMemInitParams(Map<String, ?> raw) {
        super(raw);
    }

    // ---- escalares base ----
    public int    words() { return getInt("WORDS", 0); }    // number of words
    public int priority() { return getInt("PRIORITY", 0); } // initialization priority (0=lowest)

    // ==== base validations ====
    @Override
    protected void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(abits() > 0, "ABITS must be > 0");
        require(words() > 0, "WORDS must be > 0");
        require(!memId().isEmpty(), "MEMID must be non-empty");
    }
}
