package com.cburch.logisim.verilog.layout.auxiliary;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.auxiliary.ModulePort;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;

import java.awt.*;

// Decide el tamaño de nodos ELK para cells y puertos del top
public interface NodeSizer {
    Dimension sizeForCell(Project proj, VerilogCell cell);
    Dimension sizeForTopPort(ModulePort port);
}

