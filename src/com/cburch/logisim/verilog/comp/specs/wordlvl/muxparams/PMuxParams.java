package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

/** Params para $pmux (parallel one‑hot mux). */
public final class PMuxParams extends MuxOpParams {
    public PMuxParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    public int sWidth() { return getInt("S_WIDTH", 0); }

    /** Tamaño total del bus B (WIDTH * S_WIDTH). */
    public int bTotalWidth() {
        long t = (long) width() * (long) sWidth();
        if (t > Integer.MAX_VALUE) throw new IllegalArgumentException("B total width too large");
        return (int) t;
    }

    private void validate() {
        int w = width();
        int sw = sWidth();
        require(w > 0, "WIDTH must be > 0");
        require(sw > 0, "S_WIDTH must be > 0");
        // Puertos deben respetar: |A|=|Y|=WIDTH, |S|=S_WIDTH, |B|=WIDTH*S_WIDTH (se valida en wiring).
    }

    /** Rango [lo, hi] (incl.) de B para el banco i-ésimo (0..S_WIDTH-1). */
    public int[] bSliceRange(int i) {
        require(i >= 0 && i < sWidth(), "bank index out of range: " + i);
        int w = width();
        int lo = i * w;
        int hi = lo + w - 1;
        return new int[]{lo, hi};
    }
}

