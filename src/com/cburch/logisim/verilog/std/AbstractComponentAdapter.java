package com.cburch.logisim.verilog.std;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
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
    protected static int parseIntRelaxed(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return def;
        if (s.matches("[01xXzZ]+")) {
            s = s.replaceAll("[xXzZ]", "0");
            try { return Integer.parseInt(s, 2); } catch (Exception ignore) { return def; }
        }
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
