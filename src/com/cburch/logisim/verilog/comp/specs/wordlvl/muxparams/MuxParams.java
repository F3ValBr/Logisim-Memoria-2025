package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

/**
 * Parameters for a multiplexer (MUX).
 * <p>
 * A MUX has multiple inputs, each of which is a vector of bits.
 * The output is also a vector of bits.
 * The selection input chooses which input vector to route to the output.
 * <p>
 * Parameters:
 * <ul>
 *     <li>WIDTH: Number of bits in each input and the output (default 1)</li>
 * </ul>
 */
public final class MuxParams extends MuxOpParams {
    public MuxParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    private void validate() {
        int w = width();
        require(w > 0, "WIDTH debe ser positivo");
    }

    public int sWidth() { return 1; }
}
