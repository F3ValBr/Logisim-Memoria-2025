package com.cburch.logisim.verilog.std;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;

import java.awt.*;

public abstract class AbstractComponentAdapter implements ComponentAdapter {

    @Override
    public boolean accepts(CellType type) {
        return true;
    }

    protected Component addComponent(Project proj,
                                     Circuit circ,
                                     Graphics g,
                                     ComponentFactory factory,
                                     Location where,
                                     AttributeSet attrs) throws CircuitException {
        Component comp = factory.createComponent(where, attrs);

        if (circ.hasConflict(comp)) {
            throw new CircuitException(Strings.get("exclusiveError"));
        }

        Bounds b = comp.getBounds(g);
        if (b.getX() < 0 || b.getY() < 0) {
            throw new CircuitException(Strings.get("negativeCoordError"));
        }

        CircuitMutation m = new CircuitMutation(circ);
        m.add(comp);
        proj.doAction(m.toAction(Strings.getter("addComponentAction", factory.getDisplayGetter())));
        return comp;
    }

    /** Parser tolerante (número o string decimal/binario). */
    public static int parseIntRelaxed(Object v, int def) {
        if (v == null) return def;
        try {
            if (v instanceof Number n) return n.intValue();
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return def;
            // Yosys suele dar binarios/hex en strings; intenta parsear:
            if (s.matches("^[01xXzZ]+$")) {
                // binario con posibles x/z → trata x/z como 0
                s = s.replaceAll("[xXzZ]", "0");
                return Integer.parseInt(s, 2);
            }
            if (s.startsWith("0x") || s.startsWith("0X")) return Integer.parseInt(s.substring(2), 16);
            if (s.startsWith("0b") || s.startsWith("0B")) return Integer.parseInt(s.substring(2), 2);
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    /* ===== setters “por nombre” seguros ===== */

    /** Busca un atributo por name dentro del AttributeSet. */
    protected static Attribute<?> findAttrByName(AttributeSet attrs, String name) {
        for (Attribute<?> a : attrs.getAttributes()) {
            if (name.equals(a.getName())) return a;
        }
        return null;
    }

    /** Core: parsea y setea usando Attribute.parse(token). Devuelve true si pudo setear. */
    protected static boolean setParsedByName(AttributeSet attrs, String name, String token) {
        Attribute<?> a = findAttrByName(attrs, name);
        if (a == null) return false;
        try {
            @SuppressWarnings("unchecked")
            Attribute<Object> ax = (Attribute<Object>) a;
            Object parsed = ax.parse(token);
            attrs.setValue(ax, parsed);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    /** Wrappers mínimos y legibles */
    protected static boolean setBooleanByName(AttributeSet attrs, String name, boolean value) {
        return setParsedByName(attrs, name, String.valueOf(value));
    }

    protected static boolean setOptionByName(AttributeSet attrs, String name, String optionName) {
        // optionName debe ser el “token” que reconoce parse(), p.ej. "asyncReset", "rstActiveHigh", etc.
        return setParsedByName(attrs, name, optionName);
    }

    protected static boolean setStringByName(AttributeSet attrs, String name, String value) {
        return setParsedByName(attrs, name, value);
    }

    protected static boolean setIntByName(AttributeSet attrs, String name, int value) {
        return setParsedByName(attrs, name, Integer.toString(value));
    }

    protected static boolean setHexByName(AttributeSet attrs, String name, int value) {
        return setParsedByName(attrs, name, "0x" + Integer.toHexString(value));
    }

    /** Si el token es null/blank, no hace nada (azúcar sintáctico útil). */
    protected static boolean setParsedIfPresent(AttributeSet attrs, String name, String token) {
        if (token == null || token.isBlank()) return false;
        return setParsedByName(attrs, name, token);
    }

    public static String cleanCellName(String raw) {
        if (raw == null) return "";

        String[] parts = raw.split("\\$");
        if (parts.length < 3) return raw; // fallback

        String middle = parts[1]; // entre los dos primeros $
        String last   = parts[parts.length - 1]; // después del último $

        return middle + "_" + last;
    }
}
