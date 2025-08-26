package com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams;

import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;

import java.util.Map;

/**
 * Catch-all para registros desconocidos o no implementados todavía.
 * Simplemente guarda el mapa de parámetros sin validaciones específicas.
 */
public final class GenericRegisterParams extends RegisterOpParams {
    public GenericRegisterParams(Map<String, ?> raw) {
        super(raw);
    }

    @Override
    protected void validate() {
        // Fallback: solo exigir WIDTH > 0 si existe
        int w = width();
        if (w <= 0) {
            System.err.println("[WARN] GenericRegisterParams: WIDTH no especificado o inválido.");
        }
    }
}

