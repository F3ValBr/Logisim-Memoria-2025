package com.cburch.logisim.verilog.std.macrocomponents;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;

import java.util.List;
import java.util.Map;

/** Resultado de una macro (lo que una receta construye). */
public final class Macro {
    /** Componente “visible” que representará la celda (el que devolvemos en InstanceHandle). */
    public final Component root;
    /** Todos los componentes creados (útil para debug / borrado). */
    public final List<Component> parts;
    /** Localizador de pines lógico → ubicación (si quieres enrutar por nombre de puerto). */
    public final Map<String, Location> pinMap;
    public Macro(Component root, List<Component> parts, Map<String, Location> pinMap) {
        this.root = root; this.parts = parts; this.pinMap = pinMap;
    }
}
