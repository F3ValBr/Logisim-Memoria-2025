package com.cburch.logisim.verilog.std.adapters.wordlvl;

// MuxOpAdapter.java

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.FactoryLookup;
import com.cburch.logisim.verilog.comp.auxiliary.SupportsFactoryLookup;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOp;
import com.cburch.logisim.verilog.std.*;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;

import java.awt.Graphics;

public final class MuxOpAdapter extends AbstractComponentAdapter
                                implements SupportsFactoryLookup {

    private final ModuleBlackBoxAdapter fallback = new ModuleBlackBoxAdapter();

    @Override
    public boolean accepts(CellType t) {
        return t != null && t.isWordLevel() && t.isMultiplexer();
    }

    @Override
    public InstanceHandle create(Canvas canvas, Graphics g, VerilogCell cell, Location where) {
        final MuxOp op;
        try {
            op = MuxOp.fromYosys(cell.type().typeId());
        } catch (Exception e) {
            // tipo desconocido → fallback
            return fallback.create(canvas, g, cell, where);
        }

        ComponentFactory factory = pickFactoryOrNull(canvas.getProject(), op);
        if (factory == null) {
            // No hay mapeo nativo → subcircuito
            return fallback.create(canvas, g, cell, where);
        }

        int width = guessWidth(cell.params()); // WIDTH de datos (si existe)

        try {
            Project proj = canvas.getProject();
            Circuit circ = canvas.getCircuit();

            AttributeSet attrs = factory.createAttributeSet();

            // Intentar fijar ancho de bus (cuando el factory expose StdAttr.WIDTH)
            try {
                attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
            } catch (Exception ignore) { }

            // Etiqueta visible
            try {
                attrs.setValue(StdAttr.LABEL, cleanCellName(cell.name()));
            } catch (Exception ignore) { }

            // Nota: Multiplexer/Demultiplexer en Logisim determinan #entradas/salidas con los "Select Bits".
            // Para $mux/$demux de 2-vías, suele ser el valor por defecto (1). Si quisieras setearlo:
            // usa el atributo de “Select Bits” si tu build lo expone. Lo omitimos para mantener compatibilidad.

            Component comp = addComponent(proj, circ, g, factory, where, attrs);
            PinLocator pins = (port, bit) -> comp.getLocation(); // placeholder

            return new InstanceHandle(comp, pins);
        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir " + op + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ComponentFactory peekFactory(Project proj, VerilogCell cell) {
        MuxOp op = MuxOp.fromYosys(cell.type().typeId());
        return pickFactoryOrNull(proj, op);
    }

    /** Selecciona el ComponentFactory nativo de Logisim para cada op soportada. */
    private static ComponentFactory pickFactoryOrNull(Project proj, MuxOp op) {
        return switch (op) {
            case MUX -> {
                Library plex = proj.getLogisimFile().getLibrary("Plexers");
                yield FactoryLookup.findFactory(plex, "Multiplexer");
            }
            case DEMUX -> {
                Library plex = proj.getLogisimFile().getLibrary("Plexers");
                yield FactoryLookup.findFactory(plex, "Demultiplexer");
            }
            case TRIBUF -> {
                Library wiring = proj.getLogisimFile().getLibrary("Gates");
                yield FactoryLookup.findFactory(wiring, "Controlled Buffer");
            }
            case BWMUX -> {
                Library plex = proj.getLogisimFile().getLibrary("Yosys Components");
                yield FactoryLookup.findFactory(plex, "Bitwise Multiplexer");
            }
            case PMUX -> {
                Library plex = proj.getLogisimFile().getLibrary("Yosys Components");
                yield FactoryLookup.findFactory(plex, "Priority Multiplexer");
            }
            case BMUX -> {
                Library plex = proj.getLogisimFile().getLibrary("Yosys Components");
                yield FactoryLookup.findFactory(plex, "Binary Multiplexer");
            }
        };
    }

    private static int guessWidth(CellParams params) {
        if (params instanceof GenericCellParams g) {
            Object w = g.asMap().get("WIDTH");
            int width = parseIntRelaxed(w, 1);
            return Math.max(1, width);
        }
        return 1;
    }
}

