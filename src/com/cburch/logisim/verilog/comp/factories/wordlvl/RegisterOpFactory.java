package com.cburch.logisim.verilog.comp.factories.wordlvl;

import com.cburch.logisim.verilog.comp.AbstractVerilogCellFactory;
import com.cburch.logisim.verilog.comp.VerilogCell;
import com.cburch.logisim.verilog.comp.VerilogCellFactory;
import com.cburch.logisim.verilog.comp.WordLvlCellImpl;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.specs.RegisterAttribs;
import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOp;
import com.cburch.logisim.verilog.comp.specs.wordlvl.RegisterOpParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.*;
import com.cburch.logisim.verilog.comp.specs.wordlvl.registerparams.dffeparams.*;

import java.util.*;

public class RegisterOpFactory extends AbstractVerilogCellFactory implements VerilogCellFactory {
    @Override
    public VerilogCell create(String name, String typeId,
                              Map<String,String> parameters,
                              Map<String,Object> attributes,
                              Map<String,String> portDirections,
                              Map<String, List<Object>> connections) {

        RegisterOp op = RegisterOp.fromYosys(typeId);

        RegisterOpParams params = switch (op) {
            // base
            case DFF    -> new DFFParams(parameters);
            case ADFF   -> new ADFFParams(parameters);
            case ALDFF  -> new AlDFFParams(parameters);
            case DFFSR  -> new DFFSRParams(parameters);
            case SDFF   -> new SDFFParams(parameters);

            // + enable
            case DFFE   -> new DFFEParams(parameters);
            case ADFFE  -> new ADFFEParams(parameters);
            case ALDFFE -> new AlDFFEParams(parameters);
            case DFFSRE -> new DFFSREParams(parameters);
            case SDFFE  -> new SDFFEParams(parameters);
            case SDFFCE -> new SDFFCEParams(parameters);

            default     -> new GenericRegisterParams(parameters); // comodin
        };

        var attribs = new RegisterAttribs(attributes);
        VerilogCell cell = new WordLvlCellImpl(name, CellType.fromYosys(typeId), params, attribs);

        buildEndpoints(cell, portDirections, connections);

        // -------- Validaciones comunes --------
        int w = params.width();
        switch (op) {
            case SDFF, SDFFE, SDFFCE -> {
                requirePortWidth(cell, "CLK",  1);
                requirePortWidth(cell, "SRST", 1);
                requirePortWidth(cell, "D",    w);
                requirePortWidth(cell, "Q",    w);
            }
        }

        // -------- Validaciones específicas --------
        if (op == RegisterOp.SDFFE) {
            // EN debe existir y ser 1 bit
            // En algunos dumps podría llamarse "EN", en otros "CE".
            if (hasPort(cell, "EN"))      requirePortWidth(cell, "EN", 1);
            else if (hasPort(cell, "CE")) requirePortWidth(cell, "CE", 1);
            else throw new IllegalStateException(cell.name()+": SDFFE requiere puerto EN (o CE) de 1 bit");
        }

        if (op == RegisterOp.SDFFCE) {
            if (hasPort(cell, "CE"))      requirePortWidth(cell, "CE", 1);
            else if (hasPort(cell, "EN")) requirePortWidth(cell, "EN", 1);
            else throw new IllegalStateException(cell.name()+": SDFFCE requiere puerto CE (o EN) de 1 bit");
        }

        return cell;
    }

    private static void requirePortWidth(VerilogCell cell, String port, int expected) {
        int got = cell.portWidth(port);
        if (got != expected) {
            throw new IllegalStateException(cell.name() + ": port " + port +
                    " width mismatch. expected=" + expected + " got=" + got);
        }
    }

    private static boolean hasPort(VerilogCell cell, String port) {
        return cell.getPortNames().contains(port);
    }
}
