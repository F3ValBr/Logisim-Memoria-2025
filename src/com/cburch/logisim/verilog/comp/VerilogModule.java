package com.cburch.logisim.verilog.comp;

import com.cburch.logisim.instance.Port;
import com.cburch.logisim.verilog.comp.auxiliary.ModulePort;

import java.util.List;

/**
 * Represents a Verilog module with a name, ports, and cells.
 * Provides methods to access the name, ports, and cells,
 * as well as to add new ports and cells.
 */
public interface VerilogModule {
    String name();
    List<ModulePort> ports();
    List<VerilogCell> cells();

    void addCell(VerilogCell cell);
    void addModulePort(ModulePort p);
}