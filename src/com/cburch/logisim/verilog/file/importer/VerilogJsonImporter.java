package com.cburch.logisim.verilog.file.importer;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.verilog.comp.CellFactoryRegistry;
import com.cburch.logisim.verilog.comp.auxiliary.*;
import com.cburch.logisim.verilog.comp.auxiliary.netconn.Direction;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleBuilder;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleImpl;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysJsonNetlist;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysModuleDTO;
import com.cburch.logisim.verilog.layout.LayoutUtils;
import com.cburch.logisim.verilog.layout.MemoryIndex;
import com.cburch.logisim.verilog.layout.ModuleNetIndex;
import com.cburch.logisim.verilog.layout.builder.LayoutBuilder;
import com.cburch.logisim.verilog.layout.builder.LayoutRunner;
import com.cburch.logisim.verilog.std.ComponentAdapterRegistry;
import com.cburch.logisim.verilog.std.Strings;
import com.cburch.logisim.verilog.std.adapters.wordlvl.BinaryOpAdapter;
import com.cburch.logisim.verilog.std.adapters.wordlvl.MuxOpAdapter;
import com.cburch.logisim.verilog.std.adapters.wordlvl.RegisterOpAdapter;
import com.cburch.logisim.verilog.std.adapters.wordlvl.UnaryOpAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.elk.graph.ElkNode;

import java.awt.Graphics;
import java.util.stream.Collectors;

public final class VerilogJsonImporter {

    private final CellFactoryRegistry registry;
    private final VerilogModuleBuilder builder;
    private final ComponentAdapterRegistry adapter = new ComponentAdapterRegistry()
            .register(new UnaryOpAdapter())
            .register(new BinaryOpAdapter())
            .register(new MuxOpAdapter())
            .register(new RegisterOpAdapter())
            ;

    private static final int GRID  = 10;
    private static final int MIN_X = 100;
    private static final int MIN_Y = 100;

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

        // Registro de netlist y canvas
        YosysJsonNetlist netlist = YosysJsonNetlist.from(root);
        Canvas canvas = proj.getFrame().getCanvas();
        Graphics g = canvas.getGraphics(); // si es null, el adapter usa fallback

        int totalCells = 0;

        // Recorremos módulos del netlist
        for (YosysModuleDTO dto : (Iterable<YosysModuleDTO>) netlist.modules()::iterator) {
            // Construccion del módulo
            VerilogModuleImpl mod = builder.buildModule(dto);

            System.out.println("== Módulo: " + mod.name() + " ==");
            printModulePorts(mod);

            // Creación de nets y memorias a partir de la info del módulo
            ModuleNetIndex netIndex = builder.buildNetIndex(mod);
            printNets(mod, netIndex);

            MemoryIndex memIndex = builder.buildMemoryIndex(mod);
            printMemories(memIndex);

            // Creación del layout
            LayoutBuilder.Result elk = LayoutBuilder.build(mod, netIndex);
            LayoutRunner.run(elk.root);

            // Aplicar el layout al módulo (y clamping a coordenadas positivas)
            LayoutUtils.applyLayoutAndClamp(elk.root, MIN_X, MIN_Y);

            addModulePortsToCircuit(proj, canvas.getCircuit(), mod, elk, g);

            // Por cada celda, obtener su nodo ELK y ubicar la caja en esas coordenadas
            for (VerilogCell cell : mod.cells()) {
                printCellSummary(cell);
                ElkNode n = elk.cellNode.get(cell);
                int x = snap((int)Math.round(n.getX()));
                int y = snap((int)Math.round(n.getY()));
                Location where = Location.create(x, y);

                // Instancia la caja en where y etiqueta con cell.name()
                adapter.create(canvas, g, cell, where);
                totalCells++;
            }

            System.out.println();
        }

        System.out.println("Total de celdas procesadas: " + totalCells);
        System.out.println("Done.");
    }

    /* =========================
       Helpers de impresión
       ========================= */

    private static int snap(int v){ return (v/GRID)*GRID; }

    private void addModulePortsToCircuit(Project proj,
                                         Circuit circuit,
                                         VerilogModuleImpl mod,
                                         LayoutBuilder.Result elk,
                                         Graphics g) {
        for (ModulePort p : mod.ports()) {
            ElkNode pn = elk.portNode.get(p);

            double ex = (pn != null ? pn.getX() : MIN_X);
            double ey = (pn != null ? pn.getY() : MIN_Y);

            int x = Math.max(MIN_X, snap((int)Math.round(ex)));
            int y = Math.max(MIN_Y, snap((int)Math.round(ey)));

            ComponentFactory pinFactory = Pin.FACTORY;
            AttributeSet attrs = pinFactory.createAttributeSet();

            attrs.setValue(StdAttr.WIDTH, BitWidth.create(Math.max(1, p.width())));
            attrs.setValue(Pin.ATTR_TYPE, p.direction() != Direction.INPUT);
            attrs.setValue(Pin.ATTR_TRISTATE, false);
            attrs.setValue(StdAttr.LABEL, p.name());

            Bounds ob = pinFactory.getOffsetBounds(attrs);
            // El "Location" que pasas a createComponent es el origen lógico del comp;
            // si ob.getX() es negativo, hay que empujar el location a la derecha/abajo.
            int adjX = x;
            int adjY = y;
            if (ob.getX() < 0) adjX += (MIN_X - Math.min(MIN_X, x + ob.getX()));
            if (ob.getY() < 0) adjY += (MIN_Y - Math.min(MIN_Y, y + ob.getY()));

            // Asegura seguir en grilla
            adjX = snap(adjX);
            adjY = snap(adjY);

            Location loc = Location.create(adjX, adjY);
            try {
                Component comp = addComponentSafe(proj, circuit, g, pinFactory, loc, attrs);
            } catch (CircuitException e) {
                throw new IllegalStateException("No se pudo añadir pin '" + p.name() + "': " + e.getMessage(), e);
            }
        }
    }

    private static Component addComponentSafe(Project proj,
                                              Circuit circ,
                                              Graphics g,
                                              ComponentFactory factory,
                                              Location where,
                                              AttributeSet attrs) throws CircuitException {
        Component comp = factory.createComponent(where, attrs);

        if (circ.hasConflict(comp)) {
            throw new CircuitException(Strings.get("exclusiveError"));
        }

        Bounds b = comp.getBounds(g);
        // clamp final por si acaso: si aún es negativo, corrige moviendo el location antes de fallar.
        int shiftX = 0, shiftY = 0;
        if (b.getX() < MIN_X) shiftX = MIN_X - b.getX();
        if (b.getY() < MIN_Y) shiftY = MIN_Y - b.getY();
        if (shiftX != 0 || shiftY != 0) {
            // recrear el componente movido (porque Component no expone setLocation directo)
            where = Location.create(where.getX() + snap(shiftX), where.getY() + snap(shiftY));
            comp = factory.createComponent(where, attrs);
            b = comp.getBounds(g);
        }

        if (b.getX() < 0 || b.getY() < 0) {
            throw new CircuitException(Strings.get("negativeCoordError"));
        }

        CircuitMutation m = new CircuitMutation(circ);
        m.add(comp);
        proj.doAction(m.toAction(Strings.getter("addComponentAction", factory.getDisplayGetter())));
        return comp;
    }

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
            int[] refs = idx.endpointsOf(netId).stream().mapToInt(i -> i).toArray();

            var topStrs  = new java.util.ArrayList<String>();
            var cellStrs = new java.util.ArrayList<String>();

            for (int ref : refs) {
                int bit = ModuleNetIndex.bitIdx(ref);
                if (ModuleNetIndex.isTop(ref)) {
                    int portIdx = ModuleNetIndex.ownerIdx(ref);
                    ModulePort p = mod.ports().get(portIdx);
                    topStrs.add(p.name() + "[" + bit + "]");
                } else {
                    int cellIdx = ModuleNetIndex.ownerIdx(ref);
                    VerilogCell c = mod.cells().get(cellIdx);
                    cellStrs.add(c.name() + "[" + bit + "]");
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("    net ").append(netId).append(": ");
            if (!topStrs.isEmpty())  sb.append("top=").append(topStrs).append(" ");
            if (!cellStrs.isEmpty()) sb.append("cells=").append(cellStrs);
            System.out.println(sb);
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
