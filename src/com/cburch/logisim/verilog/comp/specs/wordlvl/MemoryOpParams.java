package com.cburch.logisim.verilog.comp.specs.wordlvl;

import com.cburch.logisim.verilog.comp.specs.GenericCellParams;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Map;

public abstract class MemoryOpParams extends GenericCellParams {
    protected MemoryOpParams(Map<String, ?> raw){ super(raw); }

    public String memId() { return getString("MEMID", ""); }  // identificador de memoria
    public int    abits() { return getInt("ABITS", 0); }      // bits de dirección
    public int    width() { return getInt("WIDTH", 0); }
    public int  rdPorts() { return 0; }
    public int  wrPorts() { return 0; }
    public boolean clkEnable() { return false; }// bits por palabra

    protected void require(boolean cond, String msg){
        if(!cond) throw new IllegalArgumentException(getClass().getSimpleName()+": "+msg);
    }

    // ==== utilidades internas ====
    /** Lee un parámetro binario/numérico/hex como BitSet del tamaño esperado. Si no existe, devuelve BitSet vacío. */
    protected BitSet getMask(String key) {
        Object v = asMap().get(key);
        if (v == null) return new BitSet(0);
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return new BitSet(0);

        // Admite binario puro "00101...", decimal, o hex con 0x...
        BigInteger bi = getBigInteger(key, s);

        BitSet bs = new BitSet(bi.bitLength());
        // Convención LSB-first: bit 0 es el LSB
        for (int i = 0; i < bi.bitLength(); i++) {
            if (bi.testBit(i)) bs.set(i);
        }
        return bs;
    }

    /** Lee un parámetro binario/numérico/hex como BitSet del tamaño esperado. Si no existe, devuelve BitSet vacío. */
    protected BitSet getMask(String key, int expectedBits) {
        Object v = asMap().get(key);
        if (v == null) return new BitSet(expectedBits);
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return new BitSet(expectedBits);

        // Admite binario puro "00101...", decimal, o hex con 0x...
        BigInteger bi = getBigInteger(key, s);

        BitSet bs = new BitSet(expectedBits);
        // Convención LSB-first: bit 0 es el LSB
        for (int i = 0; i < expectedBits; i++) {
            if (bi.testBit(i)) bs.set(i);
        }
        return bs;
    }

    protected BitSet getMask(String key, int expectedBits, boolean exact) {
        Object v = asMap().get(key);
        if (v == null) return new BitSet(Math.max(expectedBits, 0));

        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return new BitSet(Math.max(expectedBits, 0));

        BigInteger bi;
        if (s.matches("[01]+")) {
            if (exact && expectedBits >= 0 && s.length() != expectedBits) {
                throw new IllegalArgumentException(key + " length mismatch: expected " + expectedBits + " got " + s.length());
            }
            bi = new BigInteger(s, 2);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            bi = new BigInteger(s.substring(2), 16);
        } else if (s.matches("\\d+")) {
            bi = new BigInteger(s, 10);
        } else {
            throw new IllegalArgumentException("Param '" + key + "' not a parsable bitmask: " + s);
        }

        int cap = (expectedBits >= 0 ? expectedBits : Math.max(expectedBitsFrom(bi), 0));
        BitSet bs = new BitSet(cap);
        int limit = (expectedBits >= 0 ? expectedBits : bi.bitLength());
        for (int i = 0; i < limit; i++) if (bi.testBit(i)) bs.set(i);
        return bs;
    }

    private static BigInteger getBigInteger(String key, String s) {
        BigInteger bi;
        if (s.matches("[01]+")) {
            bi = new BigInteger(s, 2);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            bi = new BigInteger(s.substring(2), 16);
        } else if (s.matches("\\d+")) {
            bi = new BigInteger(s, 10);
        } else {
            // TODO: algunos flujos podrían dar arrays; si ocurre, ampliar aquí (p.ej. lista de 0/1)
            throw new IllegalArgumentException("Param '"+ key +"' not a parsable bitmask: " + s);
        }
        return bi;
    }

    /** Si el parámetro existe, verifica que tenga al menos expectedBits (cuando es binario largo). */
    protected void expectMaskLenIfPresent(String key, int expectedBits) {
        Object v = asMap().get(key);
        if (v == null) return;
        String s = String.valueOf(v).trim();
        if (s.matches("[01]+")) {
            // si es binario explícito, su longitud debe igualar expectedBits
            if (s.length() != expectedBits) {
                throw new IllegalArgumentException(key + " length mismatch: expected " + expectedBits + " got " + s.length());
            }
        }
        // si es decimal/hex, no validar longitud exacta (depende del valor); el BitSet se recorta a expectedBits
    }

    private int expectedBitsFrom(BigInteger bi) {
        int bl = bi.bitLength();
        return bl == 0 ? 1 : bl;
    }

    /** Validaciones específicas de cada subtipo. */
    protected abstract void validate();
}

