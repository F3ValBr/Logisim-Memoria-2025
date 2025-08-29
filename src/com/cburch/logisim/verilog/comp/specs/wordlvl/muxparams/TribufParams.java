package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

/**
 * Parameters for a tri-state buffer (TRIBUF).
 * <p>
 * A TRIBUF has one input, one output, and one enable signal.
 * When the enable signal is true, the input is routed to the output.
 * When the enable signal is false, the output is high-impedance (Z).
 * <p>
 * Parameters:
 * <ul>
 *     <li>WIDTH: Number of bits in the input and output (default 1)</li>
 * </ul>
 */
public final class TribufParams extends MuxOpParams {
    public TribufParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    private void validate() {
        int w = width();
        require(w > 0, "WIDTH must be > 0");
    }

    public int enWidth() { return 1; }
}

