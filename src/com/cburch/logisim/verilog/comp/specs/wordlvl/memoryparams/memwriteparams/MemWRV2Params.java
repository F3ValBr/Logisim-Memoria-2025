package com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memwriteparams;

import java.util.BitSet;
import java.util.Map;

public class MemWRV2Params extends AbstractMemWRParams {
    public MemWRV2Params(Map<String, ?> raw) { super(raw); validate(); }

    // ===== Identidad y prioridad entre write-ports =====
    public int portId() { return getInt("PORTID", -1); }
    public BitSet priorityMask() { return getMask("PRIORITY_MASK"); }

    @Override
    protected void validate() {
        require(width() > 0, "WIDTH must be > 0");
        require(abits() > 0, "ABITS must be > 0");
        require(portId() >= 0, "PORTID must be >= 0");
        require(!memId().isEmpty(), "MEMID must be non-empty");

        // PRIORITY_MASK: por especificación, solo se puede tener prioridad sobre puertos con PORTID menor.
        // Si trae bits por encima de portId, lo marcamos como inválido (puedes relajarlo si tu flujo lo permite).
        BitSet pm = priorityMask();
        for (int i = portId(); i < pm.length(); i++) {
            if (pm.get(i)) {
                throw new IllegalArgumentException(
                        "PRIORITY_MASK inválida: port " + portId() +
                                " no puede tener prioridad sobre portId >= " + portId() + " (bit " + i + " activo)"
                );
            }
        }
    }
}
