package com.cburch.logisim.verilog.comp;

import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.specs.CellAttribs;
import com.cburch.logisim.verilog.comp.specs.CellParams;

public class ModuleInstanceCellImpl extends AbstractVerilogCell implements ModuleInstanceCell {
    public ModuleInstanceCellImpl(String name,
                                  CellType type,
                                  CellParams params,
                                  CellAttribs attribs) {
        super(name, type, params, attribs);
    }
}
