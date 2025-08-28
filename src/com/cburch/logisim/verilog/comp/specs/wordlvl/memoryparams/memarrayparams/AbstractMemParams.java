package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memarrayparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOpParams;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractMemParams extends MemoryOpParams {
    public AbstractMemParams(Map<String, ?> raw) { super(raw); }

    // ---- escalares base ----
    public int    size()  { return getInt("SIZE",  0); }      // # de palabras
    public int    offset(){ return getInt("OFFSET",0); }      // desplazamiento inicial (si aplica)

    /** Devuelve el INIT crudo tal como viene del JSON. */
    public String initRaw() { return getString("INIT", ""); }

    /** ¿INIT es indefinido (ej. 1'bx / x)? */
    public boolean isInitUndef() {
        String s = initRaw().trim();
        return s.equalsIgnoreCase("x") || s.equalsIgnoreCase("1'bx");
    }

    /** Intenta decodificar INIT a bits (LSB-first). Vacío si es indefinido o no está. */
    public Optional<BitSet> initBits() {
        String s = initRaw().trim();
        if (s.isEmpty() || isInitUndef()) return Optional.empty();
        try {
            BitSet bs = parseBits(s, size() * width()); // tamaño total esperado
            return Optional.of(bs);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ==== número de puertos ====
    @Override public int rdPorts()  { return getInt("RD_PORTS", 0); }
    @Override public int wrPorts()  { return getInt("WR_PORTS", 0); }

    // ---- helpers internos ----
    protected BitSet parseBits(String s, int expectedMaxBits) {
        BigInteger bi;
        if (s.matches("[01]+")) {
            bi = new BigInteger(s, 2);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            bi = new BigInteger(s.substring(2), 16);
        } else if (s.matches("\\d+")) {
            bi = new BigInteger(s, 10);
        } else {
            throw new IllegalArgumentException("INIT not parsable: " + s);
        }
        BitSet bs = new BitSet(expectedMaxBits);
        for (int i = 0; i < expectedMaxBits; i++) if (bi.testBit(i)) bs.set(i);
        return bs;
    }
}
