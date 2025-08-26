package com.cburch.logisim.verilog.comp.factories.wordlvl;

import com.cburch.logisim.verilog.comp.AbstractVerilogCellFactory;
import com.cburch.logisim.verilog.comp.VerilogCell;
import com.cburch.logisim.verilog.comp.WordLvlCellImpl;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.specs.CommonOpAttribs;
import com.cburch.logisim.verilog.comp.specs.wordlvl.UnaryOp;
import com.cburch.logisim.verilog.comp.specs.wordlvl.UnaryOpParams;

import java.util.List;
import java.util.Map;

public class UnaryOpFactory extends AbstractVerilogCellFactory {
    @Override
    public VerilogCell create(
            String name,
            String type,
            Map<String, String> params,
            Map<String, Object> attribs,
            Map<String, String> ports,
            Map<String, List<Object>> connections
    ) {
        UnaryOp op = UnaryOp.fromYosys(type);
        if (op == null) {
            throw new IllegalArgumentException("Unknown unary operation type: " + type);
        }
        UnaryOpParams parameters = new UnaryOpParams(op, params);
        CommonOpAttribs attributes = new CommonOpAttribs(attribs);
        WordLvlCellImpl cell = new WordLvlCellImpl(name, CellType.fromYosys(type), parameters, attributes);
        buildEndpoints(cell, ports, connections);
        return cell;
    }
}
