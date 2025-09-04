package com.cburch.logisim.verilog.std;

import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;

import java.awt.*;


public interface ComponentAdapter {
    boolean accepts(CellType type);
    InstanceHandle create(Canvas canvas, Graphics g, VerilogCell cell);
}
