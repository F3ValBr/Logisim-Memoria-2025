package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memreadparams;

import java.util.BitSet;
import java.util.Map;

/**
 * Parameters for memory read (MEMRD_V2).
 * <p>
 * See AbstractMemRDParams for base parameters.
 */
public class MemRDV2Params extends AbstractMemRDParams {
    public MemRDV2Params(Map<String, ?> raw) { super(raw); validate(); }

    /** Returns true if CE (chip enable) has priority over SRST (synchronous reset). */
    public boolean ceOverSrst() { return getBool("CE_OVER_SRST", false); }

    // ===== Masks against write ports (indexed by PORTID of write ports) =====
    /** Returns the transparency mask for write ports. */
    public BitSet transparencyMask() { return getMaskFlexible("TRANSPARENCY_MASK"); }
    /** Returns the collision mask for write ports. */
    public BitSet collisionXMask() { return getMaskFlexible("COLLISION_X_MASK"); }

    // Helpers to access by WR-port index if you know the total number of write ports.
    // Convention: bit i corresponds to the write-port with PORTID=i.
    /**
     * Returns true if the given write port is transparent according to the mask.
     * @param mask The BitSet mask.
     * @param wrPortId The write port index.
     * @param totalWrPorts The total number of write ports.
     */
    public boolean transparencyWithWr(BitSet mask, int wrPortId, int totalWrPorts) {
        require(wrPortId >= 0 && wrPortId < totalWrPorts, "wrPortId out of range");
        return mask.get(wrPortId);
    }

    /**
     * Returns true if the given write port has collision according to the mask.
     * @param mask The BitSet mask.
     * @param wrPortId The write port index.
     * @param totalWrPorts The total number of write ports.
     */
    public boolean collisionXWithWr(BitSet mask, int wrPortId, int totalWrPorts) {
        require(wrPortId >= 0 && wrPortId < totalWrPorts, "wrPortId out of range");
        return mask.get(wrPortId);
    }

    // ===== Reset/initial values for DATA (only for synchronous ports) =====
    /** Initial value for DATA (RD_PORTS*WIDTH in $mem_v2 combined; here is WIDTH bits of the port). */
    public BitSet initValueBits() { return getMaskExactWidth("INIT_VALUE"); }

    /** Asynchronous reset value for DATA (only used if there is synchronous logic; WIDTH bits). */
    public BitSet arstValueBits() { return getMaskExactWidth("ARST_VALUE"); }

    /** Synchronous reset value for DATA (WIDTH bits). */
    public BitSet srstValueBits() { return getMaskExactWidth("SRST_VALUE"); }

    @Override
    protected void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(abits() > 0, "ABITS must be > 0");
        require(!memId().isEmpty(), "MEMID must be non-empty");

        // If asynchronous, CLK is not used; if synchronous, it may or may not be present depending on the netlist,
        // but here we only validate parameter consistency:
        if (!clkEnable()) {
            // No additional checks; CLK may be connected as "x" in connections and is valid.
        }

        // If DATA values (INIT/ARST/SRST) exist, they must fit in WIDTH bits.
        expectMaskLenIfPresent("INIT_VALUE", width());
        expectMaskLenIfPresent("ARST_VALUE", width());
        expectMaskLenIfPresent("SRST_VALUE", width());
    }

    /**
     * Returns a mask (BitSet) of EXACT width() size if present; if not present -> empty BitSet of width bits.
     * @param key The key to look up.
     */
    private BitSet getMaskExactWidth(String key) {
        return getMask(key, width(), true);
    }

    /**
     * Returns a mask (BitSet) without checking the exact length (it is trimmed to expected if you pass -1).
     * @param key The key to look up.
     */
    private BitSet getMaskFlexible(String key) {
        return getMask(key, -1, false);
    }
}
