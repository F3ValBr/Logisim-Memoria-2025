package com.cburch.logisim.verilog.std.macrocomponents;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.verilog.std.InstanceHandle;
import com.cburch.logisim.verilog.std.adapters.BaseComposer;

import java.util.*;
import java.util.function.BiConsumer;

/** Crea/obtiene un subcircuito para una firma y lo instancia en el padre. */
public final class MacroSubcktKit extends BaseComposer {

    public static String macroName(String yosysTypeId, int... widths) {
        StringBuilder sb = new StringBuilder("ys_").append(yosysTypeId);
        if (widths != null && widths.length > 0) {
            sb.append('[');
            for (int i = 0; i < widths.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('W').append(widths[i]);
            }
            sb.append(']');
        }
        return sb.toString();
    }

    private static Circuit find(LogisimFile file, String name) {
        for (Circuit c : file.getCircuits()) if (c.getName().equals(name)) return c;
        return null;
    }

    /** Crea (si no existe) el subcircuito y ejecuta 'populate' (que debe crear también los pins). */
    public InstanceHandle ensureAndInstantiate(
            ComposeCtx ctx,
            String macroName,
            BiConsumer<ComposeCtx, Circuit> populateInternal,
            Location placeInParent,
            String labelForInstance
    ) throws CircuitException {
        LogisimFile file = ctx.proj.getLogisimFile();
        Circuit macro = find(file, macroName);
        final boolean isNew = (macro == null);

        if (macro == null) {
            macro = new Circuit(macroName);
            ctx.proj.doAction(LogisimFileActions.addCircuit(macro));

            // construir interior (pins incluidos) una sola vez
            ComposeCtx inner = new ComposeCtx(ctx.proj, macro, ctx.g, ctx.fx);
            populateInternal.accept(inner, macro);
        }

        InstanceFactory fac = macro.getSubcircuitFactory();
        AttributeSet attrs = fac.createAttributeSet();
        try { attrs.setValue(StdAttr.LABEL, labelForInstance); } catch (Exception ignore) {}

        Component inst = add(ctx, fac, placeInParent, attrs);
        return new InstanceHandle(inst, (p,b) -> inst.getLocation());
    }
}
