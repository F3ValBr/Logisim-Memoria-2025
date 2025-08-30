package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

/** Parameters for a parallel multiplexer (PMUX).
 *
 * A PMUX has multiple inputs, each of which is a vector of bits.
 * The output is also a vector of bits.
 * The selection input chooses which input vector to route to the output.
 *
 * Parameters:
 * <ul>
 *     <li>WIDTH: Number of bits in each input and the output (default 1)</li>
 *     <li>S_WIDTH: Number of input vectors (default 2)</li>
 * </ul>
 */
public final class PMuxParams extends MuxOpParams {
    public PMuxParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    public int sWidth() { return getInt("S_WIDTH", 0); }

    /** Total width of B = WIDTH * S_WIDTH */
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
        // Ports must follow: |A|=|Y|=WIDTH, |S|=S_WIDTH, |B|=WIDTH*S_WIDTH (validations on wiring).
    }

    /** Range [lo, hi] (incl.) of B for i-th bank (0..S_WIDTH-1). */
    public int[] bSliceRange(int i) {
        require(i >= 0 && i < sWidth(), "bank index out of range: " + i);
        int w = width();
        int lo = i * w;
        int hi = lo + w - 1;
        return new int[]{lo, hi};
    }
}

