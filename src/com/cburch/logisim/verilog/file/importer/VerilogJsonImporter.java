package com.cburch.logisim.verilog.file.importer;

import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.CellFactoryRegistry;
import com.cburch.logisim.verilog.comp.auxiliary.*;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleBuilder;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleImpl;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysJsonNetlist;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysModuleDTO;
import com.cburch.logisim.verilog.layout.MemoryIndex;
import com.cburch.logisim.verilog.layout.ModuleNetIndex;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.Graphics;
import java.util.stream.Collectors;

public final class VerilogJsonImporter {

    private final CellFactoryRegistry registry;
    private final VerilogModuleBuilder builder;
    private final ModuleBlackBoxAdapter adapter = new ModuleBlackBoxAdapter();

    public VerilogJsonImporter(CellFactoryRegistry registry) {
        this.registry = registry;
        this.builder = new VerilogModuleBuilder(registry);
    }

    public void importInto(Project proj) {
        System.out.println("Importing JSON Verilog...");
        JsonNode root = proj.getLogisimFile().getLoader().JSONImportChooser(proj.getFrame());
        if (root == null) {
            System.out.println("Import cancelled.");
            return;
        }

        YosysJsonNetlist netlist = YosysJsonNetlist.from(root);

        Canvas canvas = proj.getFrame().getCanvas();
        Graphics g = canvas.getGraphics(); // si es null, el adapter usa fallback

        int totalCells = 0;

        // Recorremos módulos del netlist
        for (YosysModuleDTO dto : (Iterable<YosysModuleDTO>) netlist.modules()::iterator) {
            // 1) Construir el módulo (puertos + celdas) con tu builder
            VerilogModuleImpl mod = builder.buildModule(dto);

            System.out.println("== Módulo: " + mod.name() + " ==");

            // 2) Imprimir puertos del módulo
            printModulePorts(mod);

            // 3) Instanciar CADA celda como subcircuito (caja negra) en el circuito visible
            for (VerilogCell cell : mod.cells()) {
                printCellSummary(cell);
                adapter.create(canvas, g, cell);  // ← coloca una “caja” con etiqueta = nombre de la cell
                totalCells++;
            }

            // 4) Nets internos (opcional: para debug)
            ModuleNetIndex netIndex = builder.buildNetIndex(mod);
            printNets(mod, netIndex);

            // 5) (Opcional) memorias recogidas (vía MemoryIndex)
            MemoryIndex memIndex = builder.buildMemoryIndex(mod);
            printMemories(memIndex);

            System.out.println();
        }

        System.out.println("Total de celdas procesadas: " + totalCells);
        System.out.println("Done.");
    }

    /* =========================
       Helpers de impresión
       ========================= */

    private static void printModulePorts(VerilogModuleImpl mod) {
        if (mod.ports().isEmpty()) {
            System.out.println("  (sin puertos de módulo)");
            return;
        }
        System.out.println("  Puertos:");
        for (ModulePort p : mod.ports()) {
            String bits = java.util.Arrays.stream(p.netIds())
                    .mapToObj(i -> i == ModulePort.CONST_0 ? "0" :
                            i == ModulePort.CONST_1 ? "1" :
                                    i == ModulePort.CONST_X ? "x" : String.valueOf(i))
                    .collect(Collectors.joining(","));
            System.out.println("    - " + p.name() + " : " + p.direction()
                    + " [" + p.width() + "]  bits={" + bits + "}");
        }
    }

    private static void printCellSummary(VerilogCell cell) {
        CellType ct = cell.type();
        String ports = cell.getPortNames().stream()
                .sorted()
                .map(p -> p + "[" + cell.portWidth(p) + "]")
                .collect(Collectors.joining(", "));

        System.out.println(" - Cell: " + cell.name()
                + " | typeId=" + ct.typeId()
                + " | level=" + ct.level()
                + " | kind=" + ct.kind()
                + " | ports={" + ports + "}"
        );
    }

    private static void printNets(VerilogModuleImpl mod, ModuleNetIndex idx) {
        System.out.println("  Nets:");
        for (int netId : idx.netIds()) {
            StringBuilder sb = new StringBuilder();
            sb.append("    net ").append(netId).append(": ");

            // top ports
            var tops = new java.util.ArrayList<String>();
            NetTraversal.visitNet(idx, netId, new EndpointVisitor() {
                @Override public void topPort(int portIdx, int bitIdx) {
                    ModulePort p = mod.ports().get(portIdx);
                    tops.add(p.name() + "[" + bitIdx + "]");
                }
                @Override public void cellBit(int cellIdx, int bitIdx) { /* no-op aquí */ }
            });
            if (!tops.isEmpty()) sb.append("top=").append(tops).append(" ");

            // cell endpoints
            var cells = new java.util.ArrayList<String>();
            NetTraversal.visitNet(idx, netId, new EndpointVisitor() {
                @Override public void topPort(int portIdx, int bitIdx) { /* ya listado */ }
                @Override public void cellBit(int cellIdx, int bitIdx) {
                    VerilogCell c = mod.cells().get(cellIdx);
                    cells.add(c.name() + "[" + bitIdx + "]");
                }
            });
            if (!cells.isEmpty()) sb.append("cells=").append(cells);

            System.out.println(sb.toString());
        }
    }

    private static void printMemories(MemoryIndex memIndex) {
        var all = memIndex.memories();
        if (all == null || all.isEmpty()) return;

        System.out.println("  Memories:");
        for (LogicalMemory lm : all) {
            String meta = (lm.meta() == null)
                    ? ""
                    : (" width=" + lm.meta().width()
                    + " size=" + lm.meta().size()
                    + " offset=" + lm.meta().startOffset());

            System.out.println("    - MEMID=" + lm.memId()
                    + " arrayCellIdx=" + (lm.arrayCellIdx() < 0 ? "-" : lm.arrayCellIdx())
                    + " rdPorts=" + lm.readPortIdxs().size()
                    + " wrPorts=" + lm.writePortIdxs().size()
                    + " inits=" + lm.initIdxs().size()
                    + meta);
        }
    }

}

