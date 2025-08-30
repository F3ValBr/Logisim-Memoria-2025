package com.cburch.logisim.verilog.comp.impl;

import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.specs.CellAttribs;
import com.cburch.logisim.verilog.comp.specs.CellParams;

public class WordLvlCellImpl extends AbstractVerilogCell implements WordLvlCell {
    public WordLvlCellImpl(String name,
                           CellType type,
                           CellParams params,
                           CellAttribs attribs) {
        super(name, type, params, attribs);
    }
}

