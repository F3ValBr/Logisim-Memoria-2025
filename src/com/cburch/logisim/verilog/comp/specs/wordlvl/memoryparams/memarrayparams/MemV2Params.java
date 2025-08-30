package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memarrayparams;

import java.util.BitSet;
import java.util.Map;

/**
 * Parameters for memory arrays (MEM_V2).
 * <p>
 * See AbstractMemParams for base parameters.
 */
public class MemV2Params extends AbstractMemParams {
    public MemV2Params(Map<String, ?> raw) { super(raw); validate(); }

    // ==== RD masks (by port or pair RDxWR) ====
    /** RD_WIDE_CONTINUATION: RD_PORTS bits */
    public BitSet rdWideContinuation() {
        return getMask("RD_WIDE_CONTINUATION", rdPorts());
    }

    /** RD_CLK_ENABLE: RD_PORTS bits */
    public BitSet rdClkEnable() {
        return getMask("RD_CLK_ENABLE", rdPorts());
    }

    /** RD_CLK_POLARITY: RD_PORTS bits */
    public BitSet rdClkPolarity() {
        return getMask("RD_CLK_POLARITY", rdPorts());
    }

    /** RD_TRANSPARENCY_MASK: RD_PORTS*WR_PORTS bits (flatten greater RD ,lesser WR menor, or inverse) */
    public BitSet rdTransparencyMask() {
        return getMask("RD_TRANSPARENCY_MASK", rdPorts() * wrPorts());
    }

    /** RD_COLLISION_X_MASK: RD_PORTS*WR_PORTS bits */
    public BitSet rdCollisionXMask() {
        return getMask("RD_COLLISION_X_MASK", rdPorts() * wrPorts());
    }

    /** RD_CE_OVER_SRST: RD_PORTS bits (priority CE over SRST per ports) */
    public BitSet rdCeOverSrst() {
        return getMask("RD_CE_OVER_SRST", rdPorts());
    }

    /** RD_INIT_VALUE: RD_PORTS*WIDTH bits (flatten by ports) */
    public BitSet rdInitValueBits() {
        return getMask("RD_INIT_VALUE", rdPorts() * width());
    }

    /** RD_ARST_VALUE: RD_PORTS*WIDTH bits */
    public BitSet rdArstValueBits() {
        return getMask("RD_ARST_VALUE", rdPorts() * width());
    }

    /** RD_SRST_VALUE: RD_PORTS*WIDTH bits */
    public BitSet rdSrstValueBits() {
        return getMask("RD_SRST_VALUE", rdPorts() * width());
    }

    // ==== WR masks ====
    /** WR_WIDE_CONTINUATION: WR_PORTS bits */
    public BitSet wrWideContinuation() {
        return getMask("WR_WIDE_CONTINUATION", wrPorts());
    }

    /** WR_CLK_ENABLE: WR_PORTS bits */
    public BitSet wrClkEnable() {
        return getMask("WR_CLK_ENABLE", wrPorts());
    }

    /** WR_CLK_POLARITY: WR_PORTS bits */
    public BitSet wrClkPolarity() {
        return getMask("WR_CLK_POLARITY", wrPorts());
    }

    /** WR_PRIORITY_MASK: WR_PORTS*WR_PORTS bits (flatten matrix) */
    public BitSet wrPriorityMask() {
        return getMask("WR_PRIORITY_MASK", wrPorts() * wrPorts());
    }

    // ==== access by index helpers ====

    /** Reads one RD bit by index (0..RD_PORTS-1) from an RD_PORTS mask. */
    public boolean rdFlag(BitSet mask, int rdIndex) {
        require(rdIndex >= 0 && rdIndex < rdPorts(), "rdIndex out of range");
        return mask.get(rdIndex);
    }

    /** Reads one WR bit by index (0..WR_PORTS-1) from a WR_PORTS mask. */
    public boolean wrFlag(BitSet mask, int wrIndex) {
        require(wrIndex >= 0 && wrIndex < wrPorts(), "wrIndex out of range");
        return mask.get(wrIndex);
    }

    /**
     * Accesses a RDxWR cell inside a flattened mask of length RD_PORTS*WR_PORTS.
     * Convention: index = rdIndex * WR_PORTS + wrIndex  (row=RD, column=WR).
     */
    public boolean rdWrFlag(BitSet flatMask, int rdIndex, int wrIndex) {
        require(rdIndex >= 0 && rdIndex < rdPorts(), "rdIndex out of range");
        require(wrIndex >= 0 && wrIndex < wrPorts(), "wrIndex out of range");
        int idx = rdIndex * wrPorts() + wrIndex;
        return flatMask.get(idx);
    }

    /**
     * Extracts the WIDTH vector of a RD port from a RD_PORTS*WIDTH mask (e.g. RD_INIT_VALUE).
     * Returns a BitSet of size WIDTH with the bits of the rdIndex port.
     * Convention: block i goes from [i*WIDTH .. (i+1)*WIDTH-1], LSB-first.
     */
    public BitSet rdWordBits(BitSet flat, int rdIndex) {
        require(rdIndex >= 0 && rdIndex < rdPorts(), "rdIndex out of range");
        int w = width();
        int base = rdIndex * w;
        BitSet out = new BitSet(w);
        for (int i = 0; i < w; i++) if (flat.get(base + i)) out.set(i);
        return out;
    }

    // ==== base validation ====
    @Override
    protected void validate() {
        require(!memId().isEmpty(), "MEMID must be non-empty");
        require(width()  > 0, "WIDTH must be > 0");
        require(size()   > 0, "SIZE must be > 0");
        require(abits()  > 0, "ABITS must be > 0");
        // optional: size <= 2^abits
        long cap = 1L << Math.min(abits(), 62); // avoids overflow
        require(size() <= cap, "SIZE exceeds address capacity (ABITS)");
        // if there are no ports, masks may be absent; if there are ports, validate lengths when present
        expectMaskLenIfPresent("RD_WIDE_CONTINUATION", rdPorts());
        expectMaskLenIfPresent("RD_CLK_ENABLE",       rdPorts());
        expectMaskLenIfPresent("RD_CLK_POLARITY",     rdPorts());
        expectMaskLenIfPresent("RD_CE_OVER_SRST",     rdPorts());

        expectMaskLenIfPresent("WR_WIDE_CONTINUATION", wrPorts());
        expectMaskLenIfPresent("WR_CLK_ENABLE",        wrPorts());
        expectMaskLenIfPresent("WR_CLK_POLARITY",      wrPorts());

        int rdwr = rdPorts() * wrPorts();
        expectMaskLenIfPresent("RD_TRANSPARENCY_MASK", rdwr);
        expectMaskLenIfPresent("RD_COLLISION_X_MASK",  rdwr);

        int rdw = rdPorts() * width();
        expectMaskLenIfPresent("RD_INIT_VALUE", rdw);
        expectMaskLenIfPresent("RD_ARST_VALUE", rdw);
        expectMaskLenIfPresent("RD_SRST_VALUE", rdw);

        int wrwr = wrPorts() * wrPorts();
        expectMaskLenIfPresent("WR_PRIORITY_MASK", wrwr);
    }
}


