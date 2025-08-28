package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memreadparams;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Map;

public class MemRDV2Params extends AbstractMemRDParams {
    public MemRDV2Params(Map<String, ?> raw) { super(raw); validate(); }

    public boolean ceOverSrst() { return getBool("CE_OVER_SRST", false); }

    // ===== Máscaras contra puertos de escritura (indexadas por PORTID de write ports) =====
    public BitSet transparencyMask() { return getMaskFlexible("TRANSPARENCY_MASK"); }
    public BitSet collisionXMask() { return getMaskFlexible("COLLISION_X_MASK"); }

    // Helpers para acceder por índice de WR-port si conoces la cantidad total de write ports.
    // Convención: bit i corresponde al write-port con PORTID=i.
    public boolean transparencyWithWr(BitSet mask, int wrPortId, int totalWrPorts) {
        require(wrPortId >= 0 && wrPortId < totalWrPorts, "wrPortId fuera de rango");
        return mask.get(wrPortId);
    }
    public boolean collisionXWithWr(BitSet mask, int wrPortId, int totalWrPorts) {
        require(wrPortId >= 0 && wrPortId < totalWrPorts, "wrPortId fuera de rango");
        return mask.get(wrPortId);
    }

    // ===== Valores de reset/initial del DATA (solo para puertos síncronos) =====
    /** Valor inicial del DATA (RD_PORTS*WIDTH en $mem_v2 combinado; aquí es WIDTH bits del puerto). */
    public BitSet initValueBits() { return getMaskExactWidth("INIT_VALUE"); }

    /** Valor de reset asíncrono del DATA (solo usado si hay lógica síncrona; WIDTH bits). */
    public BitSet arstValueBits() { return getMaskExactWidth("ARST_VALUE"); }

    /** Valor de reset síncrono del DATA (WIDTH bits). */
    public BitSet srstValueBits() { return getMaskExactWidth("SRST_VALUE"); }

    @Override
    protected void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(abits() > 0, "ABITS must be > 0");
        require(!memId().isEmpty(), "MEMID must be non-empty");

        // Si es asíncrono, CLK no se usa; si es síncrono, podría venir o no según netlist,
        // pero aquí solo validamos coherencia de parámetros:
        if (!clkEnable()) {
            // nada adicional; CLK puede venir conectado como "x" en connections y es válido
        }

        // Si existen valores de DATA (INIT/ARST/SRST), deben caber en WIDTH bits.
        expectMaskLenIfPresent("INIT_VALUE", width());
        expectMaskLenIfPresent("ARST_VALUE", width());
        expectMaskLenIfPresent("SRST_VALUE", width());
    }

    /** Devuelve una máscara (BitSet) de tamaño EXACTO width() si existe; si no existe -> BitSet vacío de width bits. */
    private BitSet getMaskExactWidth(String key) {
        return getMask(key, width(), true);
    }

    /** Devuelve una máscara (BitSet) sin chequear longitud exacta (se recorta a expected si lo pasas -1). */
    private BitSet getMaskFlexible(String key) {
        return getMask(key, -1, false);
    }
}
