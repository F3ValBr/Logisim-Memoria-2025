package com.cburch.logisim.verilog.std.adapters;

import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.std.Strings;
import com.cburch.logisim.verilog.std.macrocomponents.ComposeCtx;

import static com.cburch.logisim.verilog.std.AbstractComponentAdapter.cleanCellName;

/** Base con helpers compartidos para todos los compositores. */
public abstract class BaseComposer {
    protected Component add(ComposeCtx ctx,
                            ComponentFactory f,
                            Location loc,
                            AttributeSet attrs)
            throws CircuitException {
        Component comp = f.createComponent(loc, attrs);

        if (ctx.circ.hasConflict(comp)) {
            throw new CircuitException(Strings.get("exclusiveError"));
        }

        Bounds b = comp.getBounds(ctx.g);
        if (b.getX() < 0 || b.getY() < 0) {
            throw new CircuitException(Strings.get("negativeCoordError"));
        }

        CircuitMutation m = new CircuitMutation(ctx.circ);
        m.add(comp);
        ctx.proj.doAction(m.toAction(Strings.getter("addComponentAction", f.getDisplayGetter())));
        return comp;
    }
    protected String lbl(VerilogCell cell){ return cleanCellName(cell.name()); }
}
