package com.cburch.logisim.verilog.comp.specs;

import java.util.Map;

public final class ModuleAttribs extends GenericCellAttribs {

    public ModuleAttribs(Map<String, ?> raw) {
        super(raw);
    }

    /**
     * Indica si este módulo fue marcado por Yosys como "no derivado".
     * Yosys coloca el atributo "module_not_derived": "000...1".
     */
    public boolean isModuleNotDerived() {
        return getBool("module_not_derived", false);
    }

    /**
     * Devuelve la ubicación de origen en el código RTL (ej: "factorial.sv:13.19-13.65").
     */
    public String source() {
        return getString("src", "unknown");
    }

    // Puedes añadir más helpers si usas otros atributos de Yosys
}
