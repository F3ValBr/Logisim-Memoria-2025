package com.cburch.logisim.verilog.std.adapters;


import com.cburch.logisim.circuit.*;
import com.cburch.logisim.comp.Component;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.std.ComponentAdapter;
import com.cburch.logisim.verilog.std.InstanceHandle;
import com.cburch.logisim.verilog.std.PinLocator;
import com.cburch.logisim.verilog.std.Strings;

import java.awt.*;

public final class ModuleBlackBoxAdapter implements ComponentAdapter {

    @Override public boolean accepts(CellType t) { return true; } // fallback universal

    @Override
    public InstanceHandle create(Canvas canvas, Graphics g, VerilogCell cell, Location where) {
        try {
            Project proj = canvas.getProject();
            Circuit newCirc = new Circuit(safeName(cell.name()));
            Circuit currentCirc = canvas.getCircuit();

            proj.doAction(LogisimFileActions.addCircuit(newCirc));

            InstanceFactory f = new SubcircuitFactory(newCirc);
            AttributeSet attrs = f.createAttributeSet();

            attrs.setValue(StdAttr.LABEL, cell.typeId());
            attrs.setValue(CircuitAttributes.LABEL_LOCATION_ATTR, Direction.NORTH);

            // 4) Añadir con acción (undo/redo)
            Component comp = f.createComponent(where, attrs);

            if (currentCirc.hasConflict(comp)) {
                throw new CircuitException(Strings.get("exclusiveError"));
            }

            Bounds bds = comp.getBounds(g);
            if (bds.getX() < 0 || bds.getY() < 0) {
                throw new CircuitException(Strings.get("negativeCoordError"));
            }

            // 5) Añadir con acción (undo/redo)
            CircuitMutation m = new CircuitMutation(currentCirc);
            m.add(comp);
            proj.doAction(m.toAction(Strings.getter("addComponentAction", f.getDisplayGetter())));

            // 6) PinLocator simple
            PinLocator pins = (port, bit) -> comp.getLocation();
            return new InstanceHandle(comp, pins);
        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir subcircuito: " + e.getMessage(), e);
        }
    }

    private static String safeName(String n) {
        return (n == null || n.isBlank()) ? "unnamed" : n;
    }
}

