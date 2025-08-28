package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memreadparams;

import java.util.BitSet;
import java.util.Map;

public class MemRDParams extends AbstractMemRDParams {
    public MemRDParams(Map<String, ?> raw) { super(raw); validate(); }

    public BitSet transparent() { return getMaskFlexible("TRANSPARENT"); }

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
    }

    /** Devuelve una máscara (BitSet) sin chequear longitud exacta (se recorta a expected si lo pasas -1). */
    private BitSet getMaskFlexible(String key) {
        return getMask(key, -1, false);
    }
}
