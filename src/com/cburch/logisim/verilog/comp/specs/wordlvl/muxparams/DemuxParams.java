package com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;

import java.util.Map;

/**
 * Parameters for a demultiplexer (DEMUX).
 * <p>
 * A DEMUX has one input, which is a vector of bits.
 * The output is multiple vectors of bits.
 * The selection input chooses which output vector to route the input to.
 * <p>
 * Parameters:
 * <ul>
 *     <li>WIDTH: Number of bits in the input (default 1)</li>
 *     <li>S_WIDTH: Number of output vectors (default 2)</li>
 * </ul>
 */
public final class DemuxParams extends MuxOpParams {
    public DemuxParams(Map<String, ?> raw) {
        super(raw);
        validate();
    }

    public int sWidth() { return getInt("S_WIDTH", 0); }

    /** Total number of bits in Y = WIDTH * S_WIDTH */
    public int yTotalWidth() {
        return width() * sWidth();
    }

    private void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(sWidth() > 0, "S_WIDTH must be > 0");
    }
}

