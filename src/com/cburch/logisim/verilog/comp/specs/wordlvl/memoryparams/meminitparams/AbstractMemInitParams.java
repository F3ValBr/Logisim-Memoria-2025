package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.meminitparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOpParams;

import java.util.Map;

public abstract class AbstractMemInitParams extends MemoryOpParams {
    public AbstractMemInitParams(Map<String, ?> raw) {
        super(raw);
    }

    // ---- escalares base ----
    public String memId() { return getString("MEMID", ""); }  // identificador de memoria
    public int    abits() { return getInt("ABITS", 0); }      // bits de dirección
    public int    width() { return getInt("WIDTH", 0); }      // bits por palabra
    public int    words() { return getInt("WORDS", 0); }      // # de palabras
    public int priority() { return getInt("PRIORITY", 0); } // prioridad de la inicialización (mayor = más prioridad)

    // ==== validación base ====
    @Override
    protected void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(abits() > 0, "ABITS must be > 0");
        require(words() > 0, "WORDS must be > 0");
        require(!memId().isEmpty(), "MEMID must be non-empty");
    }
}
