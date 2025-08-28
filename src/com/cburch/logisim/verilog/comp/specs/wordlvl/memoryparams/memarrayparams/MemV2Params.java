package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memarrayparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOpParams;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Map;

public class MemV2Params extends AbstractMemParams {
    public MemV2Params(Map<String, ?> raw) { super(raw); validate(); }

    // ==== máscaras RD (por puerto o por par RDxWR) ====
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

    /** RD_TRANSPARENCY_MASK: RD_PORTS*WR_PORTS bits (aplana RD mayor, WR menor, o al revés: define convención) */
    public BitSet rdTransparencyMask() {
        return getMask("RD_TRANSPARENCY_MASK", rdPorts() * wrPorts());
    }

    /** RD_COLLISION_X_MASK: RD_PORTS*WR_PORTS bits */
    public BitSet rdCollisionXMask() {
        return getMask("RD_COLLISION_X_MASK", rdPorts() * wrPorts());
    }

    /** RD_CE_OVER_SRST: RD_PORTS bits (prioridad CE sobre SRST por puerto) */
    public BitSet rdCeOverSrst() {
        return getMask("RD_CE_OVER_SRST", rdPorts());
    }

    /** RD_INIT_VALUE: RD_PORTS*WIDTH bits (aplanado por puertos) */
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

    // ==== máscaras WR ====
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

    /** WR_PRIORITY_MASK: WR_PORTS*WR_PORTS bits (matriz aplanada) */
    public BitSet wrPriorityMask() {
        return getMask("WR_PRIORITY_MASK", wrPorts() * wrPorts());
    }

    // ==== helpers de acceso por índice (para matrices y vectores) ====

    /** Lee un bit RD por índice (0..RD_PORTS-1) de una máscara RD_PORTS. */
    public boolean rdFlag(BitSet mask, int rdIndex) {
        require(rdIndex >= 0 && rdIndex < rdPorts(), "rdIndex out of range");
        return mask.get(rdIndex);
    }

    /** Lee un bit WR por índice (0..WR_PORTS-1) de una máscara WR_PORTS. */
    public boolean wrFlag(BitSet mask, int wrIndex) {
        require(wrIndex >= 0 && wrIndex < wrPorts(), "wrIndex out of range");
        return mask.get(wrIndex);
    }

    /**
     * Acceso a una celda RDxWR dentro de una máscara aplanada de longitud RD_PORTS*WR_PORTS.
     * Convención: index = rdIndex * WR_PORTS + wrIndex  (fila=RD, columna=WR).
     */
    public boolean rdWrFlag(BitSet flatMask, int rdIndex, int wrIndex) {
        require(rdIndex >= 0 && rdIndex < rdPorts(), "rdIndex out of range");
        require(wrIndex >= 0 && wrIndex < wrPorts(), "wrIndex out of range");
        int idx = rdIndex * wrPorts() + wrIndex;
        return flatMask.get(idx);
    }

    /**
     * Extrae el vector WIDTH de un puerto RD en una máscara RD_PORTS*WIDTH (ej. RD_INIT_VALUE).
     * Devuelve un BitSet de tamaño WIDTH con los bits del puerto rdIndex.
     * Convención: bloque i va de [i*WIDTH .. (i+1)*WIDTH-1], LSB-first.
     */
    public BitSet rdWordBits(BitSet flat, int rdIndex) {
        require(rdIndex >= 0 && rdIndex < rdPorts(), "rdIndex out of range");
        int w = width();
        int base = rdIndex * w;
        BitSet out = new BitSet(w);
        for (int i = 0; i < w; i++) if (flat.get(base + i)) out.set(i);
        return out;
    }

    // ==== validación base ====
    @Override
    protected void validate() {
        require(!memId().isEmpty(), "MEMID must be non-empty");
        require(width()  > 0, "WIDTH must be > 0");
        require(size()   > 0, "SIZE must be > 0");
        require(abits()  > 0, "ABITS must be > 0");
        // opcional: size <= 2^abits
        long cap = 1L << Math.min(abits(), 62); // evita overflow
        require(size() <= cap, "SIZE exceeds address capacity (ABITS)");
        // si no hay puertos, máscaras pueden estar ausentes; si hay puertos, valida longitudes cuando existan
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


