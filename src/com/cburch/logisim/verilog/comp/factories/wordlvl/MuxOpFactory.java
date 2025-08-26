package com.cburch.logisim.verilog.comp.factories.wordlvl;

import com.cburch.logisim.verilog.comp.AbstractVerilogCellFactory;
import com.cburch.logisim.verilog.comp.VerilogCell;
import com.cburch.logisim.verilog.comp.VerilogCellFactory;
import com.cburch.logisim.verilog.comp.WordLvlCellImpl;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.specs.CommonOpAttribs;
import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOpParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams.*;

import java.util.List;
import java.util.Map;

public class MuxOpFactory extends AbstractVerilogCellFactory implements VerilogCellFactory {
    @Override
    public VerilogCell create(
            String name,
            String type,
            Map<String, String> params,
            Map<String, Object> attribs,
            Map<String, String> ports,
            Map<String, List<Object>> connections
    ) {
        MuxOpParams parameters = getMuxOpParams(type, params);
        var attributes = new CommonOpAttribs(attribs); // o GenericCellAttribs
        var cell = new WordLvlCellImpl(name, CellType.fromYosys(type), parameters, attributes);
        buildEndpoints(cell, ports, connections);

        int w = parameters.width();
        switch (type) {
            case "$mux" -> {
                // A,B,Y deben tener w bits; S 1 bit
                requirePortWidth(cell, "A", w);
                requirePortWidth(cell, "B", w);
                requirePortWidth(cell, "Y", w);
                requirePortWidth(cell, "S", 1);
            }
            case "$pmux" -> {
                PMuxParams p = (PMuxParams) parameters;
                requirePortWidth(cell, "A", w);
                requirePortWidth(cell, "Y", w);
                requirePortWidth(cell, "S", p.sWidth());
                requirePortWidth(cell, "B", p.bTotalWidth());
            }
            case "$tribuf" -> {
                requirePortWidth(cell, "A", w);
                requirePortWidth(cell, "Y", w);
                requirePortWidth(cell, "EN", 1);
            }
        }

        return cell;
    }

    private static MuxOpParams getMuxOpParams(String type, Map<String, String> params) {
        MuxOpParams parameters;
        switch (type) {
            case "$mux"   -> parameters = new MuxParams(params);
            case "$pmux"  -> parameters = new PMuxParams(params);
            case "$tribuf"-> parameters = new TribufParams(params);
            case "$bmux"  -> parameters = new BMuxParams(params);
            case "$bwmux" -> parameters = new BWMuxParams(params);
            case "$demux" -> parameters = new DemuxParams(params);
            default -> throw new IllegalArgumentException("Not a known mux-family cell: " + type);
        }
        return parameters;
    }

    private void requirePortWidth(VerilogCell cell, String port, int expected) {
        int got = cell.portWidth(port);
        if (got != expected) {
            throw new IllegalStateException(cell.name() + ": port " + port +
                    " width mismatch. expected=" + expected + " got=" + got);
        }
    }
}

