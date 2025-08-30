package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

/**
 * Parameters for a bitwise multiplexer (BMUX).
 * <p>
 * A BMUX has multiple inputs, each of which is a vector of bits.
 * The output is also a vector of bits.
 * The selection input chooses which input vector to route to the output.
 * <p>
 * Parameters:
 * <ul>
 *     <li>WIDTH: Number of bits in each input and the output (default 1)</li>
 *     <li>S_WIDTH: Number of input vectors (default 2)</li>
 * </ul>
 */
public final class BMuxParams extends MuxOpParams {
    public BMuxParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    public int sWidth() { return getInt("S_WIDTH", 0); }

    /** Total width of the output (and each input) */
    public int aTotalWidth() {
        return width() * sWidth();
    }

    private void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(sWidth() > 0, "S_WIDTH must be > 0");
    }
}
