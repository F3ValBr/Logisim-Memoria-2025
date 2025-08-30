package com.cburch.logisim.verilog.std;

import com.cburch.logisim.verilog.comp.AbstractVerilogCellFactory;
import com.cburch.logisim.verilog.comp.impl.ModuleInstanceCellImpl;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.comp.specs.ModuleAttribs;

import java.util.List;
import java.util.Map;

public class ModuleInstance extends AbstractVerilogCellFactory {
    @Override
    public VerilogCell create(
            String name,
            String type,
            Map<String, String> ports,
            Map<String, Object> params,
            Map<String, String> attribs,
            Map<String, List<Object>> connections
    ) {
        var parameters = new GenericCellParams(params);
        var attributes = new ModuleAttribs(attribs);
        var cell = new ModuleInstanceCellImpl(name, CellType.fromYosys(type), parameters, attributes);
        buildEndpoints(cell, ports, connections);
        return cell;
    }
}
