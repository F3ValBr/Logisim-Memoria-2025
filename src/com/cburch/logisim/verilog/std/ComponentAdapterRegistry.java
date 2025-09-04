package com.cburch.logisim.verilog.std;

import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class ComponentAdapterRegistry {
    private final List<ComponentAdapter> adapters = new ArrayList<>();
    public ComponentAdapterRegistry register(ComponentAdapter a){ adapters.add(a); return this; }
    public InstanceHandle create(Canvas canvas, Graphics g, VerilogCell cell) {
        for(ComponentAdapter a : adapters){
            if(a.accepts(cell.type())) {
                return a.create(canvas, g, cell);
            }
        }
        // fallback universal
        return new ModuleBlackBoxAdapter().create(canvas, g, cell);
    }
}