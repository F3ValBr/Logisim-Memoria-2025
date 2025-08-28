package com.cburch.logisim.verilog.comp.factories.wordlvl;

import com.cburch.logisim.verilog.comp.AbstractVerilogCellFactory;
import com.cburch.logisim.verilog.comp.VerilogCell;
import com.cburch.logisim.verilog.comp.VerilogCellFactory;
import com.cburch.logisim.verilog.comp.WordLvlCellImpl;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.specs.GenericCellAttribs;
import com.cburch.logisim.verilog.comp.specs.wordlvl.MemoryOp;
import com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memarrayparams.MemParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memarrayparams.MemV2Params;
import com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.meminitparams.MemInitParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memreadparams.MemRDParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memreadparams.MemRDV2Params;
import com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memwriteparams.MemWRParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.memoryparams.memwriteparams.MemWRV2Params;

import java.util.List;
import java.util.Map;

public class MemoryOpFactory extends AbstractVerilogCellFactory implements VerilogCellFactory {

    @Override
    public VerilogCell create(String name,
                              String typeId,
                              Map<String, String> parameters,
                              Map<String, Object> attributes,
                              Map<String, String> portDirections,
                              Map<String, List<Object>> connections) {

        CellType ct = CellType.fromYosys(typeId);
        GenericCellAttribs attribs = new GenericCellAttribs(attributes);

        switch (typeId) {
            /* ======================
               Arreglo de memoria
               ====================== */
            case "$mem" -> {
                MemParams p = new MemParams(parameters);
                VerilogCell cell = new WordLvlCellImpl(name, ct, p, attribs);
                buildEndpoints(cell, portDirections, connections);

                // Validaciones típicas para aplanado por puertos (ajusta a tu dump si usa *_0, *_1, ...)
                requirePortWidthOptional(cell, "RD_ADDR", p.abits() * p.rdPorts());
                requirePortWidthOptional(cell, "RD_DATA", p.width() * p.rdPorts());
                requirePortWidthOptional(cell, "WR_ADDR", p.abits() * p.wrPorts());
                requirePortWidthOptional(cell, "WR_DATA", p.width() * p.wrPorts());
                // EN por-bit o por-puerto: asume por-bit (WIDTH*WR_PORTS). Cambia a p.wrPorts() si tu dump usa 1 por puerto.
                if (hasPort(cell, "WR_EN")) {
                    requirePortWidth(cell, "WR_EN", p.width() * p.wrPorts());
                }
                return cell;
            }

            case "$mem_v2" -> {
                MemV2Params p = new MemV2Params(parameters);
                VerilogCell cell = new WordLvlCellImpl(name, ct, p, attribs);
                buildEndpoints(cell, portDirections, connections);

                // En v2, muchos campos vienen por-puerto y el wiring puede estar aplanado.
                requirePortWidthOptional(cell, "RD_ADDR", p.abits() * p.rdPorts());
                requirePortWidthOptional(cell, "RD_DATA", p.width() * p.rdPorts());
                requirePortWidthOptional(cell, "WR_ADDR", p.abits() * p.wrPorts());
                requirePortWidthOptional(cell, "WR_DATA", p.width() * p.wrPorts());
                if (hasPort(cell, "WR_EN")) {
                    requirePortWidth(cell, "WR_EN", p.width() * p.wrPorts());
                }
                return cell;
            }

            /* ======================
               Puertos de lectura
               ====================== */
            case "$memrd" -> {
                MemRDParams p = new MemRDParams(parameters);
                VerilogCell cell = new WordLvlCellImpl(name, ct, p, attribs);
                buildEndpoints(cell, portDirections, connections);

                requirePortWidth(cell, "ADDR", p.abits());
                requirePortWidth(cell, "DATA", p.width());
                // EN/CLK pueden venir como "x" si no se usan:
                if (hasPort(cell, "EN"))  requirePortWidth(cell, "EN", 1);
                if (p.clkEnable() && hasPort(cell, "CLK")) requirePortWidth(cell, "CLK", 1);
                // ARST/SRST normalmente no existen en v1 de memrd; omite validación aquí.
                return cell;
            }

            case "$memrd_v2" -> {
                MemRDV2Params p = new MemRDV2Params(parameters);
                VerilogCell cell = new WordLvlCellImpl(name, ct, p, attribs);
                buildEndpoints(cell, portDirections, connections);

                requirePortWidth(cell, "ADDR", p.abits());
                requirePortWidth(cell, "DATA", p.width());
                if (hasPort(cell, "EN"))  requirePortWidth(cell, "EN", 1);
                if (p.clkEnable() && hasPort(cell, "CLK")) requirePortWidth(cell, "CLK", 1);
                // ARST/SRST son entradas 1-bit si están presentes
                if (hasPort(cell, "ARST")) requirePortWidth(cell, "ARST", 1);
                if (hasPort(cell, "SRST")) requirePortWidth(cell, "SRST", 1);
                return cell;
            }

            /* ======================
               Puertos de escritura
               ====================== */
            case "$memwr" -> {
                MemWRParams p = new MemWRParams(parameters);
                VerilogCell cell = new WordLvlCellImpl(name, ct, p, attribs);
                buildEndpoints(cell, portDirections, connections);

                requirePortWidth(cell, "ADDR", p.abits());
                requirePortWidth(cell, "DATA", p.width());
                // EN por-bit → mismo ancho que DATA
                if (hasPort(cell, "EN")) requirePortWidth(cell, "EN", p.width());
                if (p.clkEnable() && hasPort(cell, "CLK")) requirePortWidth(cell, "CLK", 1);
                return cell;
            }

            case "$memwr_v2" -> {
                MemWRV2Params p = new MemWRV2Params(parameters);
                VerilogCell cell = new WordLvlCellImpl(name, ct, p, attribs);
                buildEndpoints(cell, portDirections, connections);

                requirePortWidth(cell, "ADDR", p.abits());
                requirePortWidth(cell, "DATA", p.width());
                if (hasPort(cell, "EN")) requirePortWidth(cell, "EN", p.width()); // enable por-bit
                if (p.clkEnable() && hasPort(cell, "CLK")) requirePortWidth(cell, "CLK", 1);
                return cell;
            }

            /* ======================
               Inicialización
               ====================== */
            case "$meminit", "$meminit_v2" -> {
                MemInitParams p = new MemInitParams(parameters);
                VerilogCell cell = new WordLvlCellImpl(name, ct, p, attribs);
                buildEndpoints(cell, portDirections, connections);
                // Normalmente no tiene puertos, solo parámetros INIT/...; sin validaciones de wiring aquí.
                return cell;
            }

            default -> throw new IllegalArgumentException("MemoryOpFactory: tipo no soportado: " + typeId);
        }
    }

    private static void requirePortWidth(VerilogCell cell, String port, int expected) {
        int got = cell.portWidth(port);
        if (got != expected) {
            throw new IllegalStateException(cell.name() + ": port " + port +
                    " width mismatch. expected=" + expected + " got=" + got);
        }
    }

    private static void requirePortWidthOptional(VerilogCell cell, String port, int expected) {
        if (hasPort(cell, port)) requirePortWidth(cell, port, expected);
    }

    private static boolean hasPort(VerilogCell cell, String port) {
        return cell.getPortNames().contains(port);
    }
}
