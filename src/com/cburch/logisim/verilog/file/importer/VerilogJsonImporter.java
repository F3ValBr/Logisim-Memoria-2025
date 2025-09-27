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
import com.cburch.logisim.verilog.layout.auxiliary.DefaultNodeSizer;
import com.cburch.logisim.verilog.layout.auxiliary.NodeSizer;
import com.cburch.logisim.verilog.layout.builder.LayoutBuilder;
import com.cburch.logisim.verilog.layout.builder.LayoutRunner;
import com.cburch.logisim.verilog.std.ComponentAdapterRegistry;
import com.cburch.logisim.verilog.std.Strings;
import com.cburch.logisim.verilog.std.adapters.wordlvl.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.elk.graph.ElkNode;

import java.awt.Graphics;
import java.util.*;
import java.util.stream.Collectors;

public final class VerilogJsonImporter {

    private final CellFactoryRegistry registry;
    private final VerilogModuleBuilder builder;
    private final MemoryOpAdapter memoryAdapter = new MemoryOpAdapter();
    private final ComponentAdapterRegistry adapter = new ComponentAdapterRegistry()
            .register(new UnaryOpAdapter())
            .register(new BinaryOpAdapter())
            .register(new MuxOpAdapter())
            .register(new RegisterOpAdapter())
            .register(memoryAdapter)
            ;
    NodeSizer sizer = new DefaultNodeSizer(adapter);

    private static final int GRID  = 10;
    private static final int MIN_X = 100;
    private static final int MIN_Y = 100;
    private static final int PAD_X = 100; // separación horizontal respecto a las celdas

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
        Graphics g = canvas.getGraphics(); // si es null, los adapters usan fallback

        int totalCells = 0;

        // Recorremos módulos del netlist
        for (YosysModuleDTO dto : (Iterable<YosysModuleDTO>) netlist.modules()::iterator) {
            // Construcción del módulo
            VerilogModuleImpl mod = builder.buildModule(dto);

            System.out.println("== Módulo: " + mod.name() + " ==");
            printModulePorts(mod);

            // Netlist + memorias
            ModuleNetIndex netIndex = builder.buildNetIndex(mod);
            printNets(mod, netIndex);

            MemoryIndex memIndex = builder.buildMemoryIndex(mod);
            memoryAdapter.beginModule(memIndex, mod); // contexto de memorias
            printMemories(memIndex);

            // ===== 1) Construir alias de celdas ($memrd/$memwr/$meminit → representante) =====
            Map<VerilogCell, VerilogCell> cellAlias = buildMemoryCellAlias(mod, memIndex);

            // ===== 2) Layout con alias (no crea nodos para los “alias”) =====
            LayoutBuilder.Result elk = LayoutBuilder.build(proj, mod, netIndex, sizer, cellAlias);
            LayoutRunner.run(elk.root);

            // Aplicar el layout al módulo (y clamping a coordenadas positivas)
            LayoutUtils.applyLayoutAndClamp(elk.root, MIN_X, MIN_Y);

            // Puertos top
            addModulePortsToCircuitSeparated(proj, canvas.getCircuit(), mod, elk, netIndex, g);

            // ===== 3) Instanciar solo celdas no-aliased (i.e., representantes o celdas “normales”) =====
            for (int i = 0; i < mod.cells().size(); i++) {
                VerilogCell cell = mod.cells().get(i);
                if (cellAlias.containsKey(cell)) continue; // es un alias; su representante ya tiene nodo

                printCellSummary(cell);
                ElkNode n = elk.cellNode.get(cell);
                int x;
                int y;
                if (n == null) {
                    // Fallback raro: puede ocurrir si una memoria sin puertos quedó sin aristas
                    // Ubícala en algún lugar válido de grilla.
                    x = snap(MIN_X);
                    y = snap(MIN_Y);
                } else {
                    x = snap((int) Math.round(n.getX()));
                    y = snap((int) Math.round(n.getY()));
                }
                adapter.create(canvas, g, cell, Location.create(x, y));
                totalCells++;
            }

            System.out.println();
        }

        System.out.println("Total de celdas procesadas: " + totalCells);
        System.out.println("Done.");
    }

    /* ===================== Helpers ===================== */

    /** Construye el mapa de alias: cada $memrd/$memwr/$meminit apunta a su representante. */
    private static Map<VerilogCell, VerilogCell> buildMemoryCellAlias(VerilogModuleImpl mod, MemoryIndex memIndex) {
        Map<VerilogCell, VerilogCell> alias = new HashMap<>();

        for (LogicalMemory lm : memIndex.memories()) {
            int arrIdx = lm.arrayCellIdx();
            VerilogCell rep = null;

            if (arrIdx >= 0) {
                rep = mod.cells().get(arrIdx); // hay $mem / $mem_v2 → representante natural
            } else {
                // no hay array: toma el primer rd o wr como representante lógico
                Integer idx = !lm.readPortIdxs().isEmpty()
                        ? lm.readPortIdxs().get(0)
                        : (!lm.writePortIdxs().isEmpty() ? lm.writePortIdxs().get(0) : null);
                if (idx != null) rep = mod.cells().get(idx);
            }

            if (rep == null) continue; // nada que aliasar

            // Aliasa todos los demás a rep
            for (Integer i : lm.readPortIdxs())  {
                VerilogCell c = mod.cells().get(i);
                if (c != rep) alias.put(c, rep);
            }
            for (Integer i : lm.writePortIdxs()) {
                VerilogCell c = mod.cells().get(i);
                if (c != rep) alias.put(c, rep);
            }
            for (Integer i : lm.initIdxs()) {
                VerilogCell c = mod.cells().get(i);
                if (c != rep) alias.put(c, rep);
            }
        }

        return alias;
    }

    private static int snap(int v){ return (v/GRID)*GRID; }

    private void addModulePortsToCircuitSeparated(Project proj,
                                                  Circuit circuit,
                                                  VerilogModuleImpl mod,
                                                  LayoutBuilder.Result elk,
                                                  ModuleNetIndex netIdx,
                                                  Graphics g) {

        // 1) Caja envolvente de las celdas del módulo
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (VerilogCell c : mod.cells()) {
            ElkNode n = elk.cellNode.get(c);
            if (n == null) continue;
            int x0 = (int)Math.round(n.getX());
            int y0 = (int)Math.round(n.getY());
            int x1 = x0 + (int)Math.round(n.getWidth());
            int y1 = y0 + (int)Math.round(n.getHeight());
            minX = Math.min(minX, x0);
            minY = Math.min(minY, y0);
            maxX = Math.max(maxX, x1);
            maxY = Math.max(maxY, y1);
        }
        if (minX == Integer.MAX_VALUE) { // módulo vacío: fallback
            minX = MIN_X; minY = MIN_Y; maxX = MIN_X + 100; maxY = MIN_Y + 100;
        }

        // 2) Separación horizontal
        int xInputs  = snap(minX - PAD_X);
        int xOutputs = snap(maxX + PAD_X);

        // 3) Fila vertical para inputs (simple): repartir entre minY..maxY
        List<ModulePort> inputs  = mod.ports().stream()
                .filter(p -> p.direction() == Direction.INPUT).toList();
        List<ModulePort> outputs = mod.ports().stream()
                .filter(p -> p.direction() == Direction.OUTPUT).toList();

        int spanY = Math.max(1, maxY - minY);
        int inStep = Math.max(GRID, spanY / Math.max(1, inputs.size()+1));
        int curInY = minY + inStep;

        // 4) Para outputs, calculamos Y a partir de los nodos internos conectados
        Map<ModulePort,Integer> outY = new HashMap<>();
        for (ModulePort p : outputs) {
            // Recolectar Y de nodos internos conectados a CUALQUIER bit del puerto
            List<Integer> ys = new ArrayList<>();

            // Para cada net del módulo, si contiene un endpoint top de este puerto, añade
            for (int netId : netIdx.netIds()) {
                List<Integer> refs = netIdx.endpointsOf(netId);
                if (refs == null || refs.isEmpty()) continue;

                boolean touchesThisPort = false;
                for (int ref : refs) {
                    if (ModuleNetIndex.isTop(ref)) {
                        int pIdx = netIdx.resolveTopPortIdx(ref);
                        if (pIdx >= 0 && pIdx < mod.ports().size() && mod.ports().get(pIdx) == p) {
                            touchesThisPort = true;
                            break;
                        }
                    }
                }
                if (!touchesThisPort) continue;

                // Extrae Y de los endpoints internos de esta net
                for (int ref : refs) {
                    if (!ModuleNetIndex.isTop(ref)) {
                        int cellIdx = ModuleNetIndex.ownerIdx(ref);
                        VerilogCell cell = mod.cells().get(cellIdx);
                        ElkNode n = elk.cellNode.get(cell);
                        if (n != null) {
                            int y = snap((int)Math.round(n.getY() + n.getHeight()/2.0));
                            ys.add(y);
                        }
                    }
                }
            }

            if (!ys.isEmpty()) {
                ys.sort(Integer::compare);
                int y;
                int m = ys.size();
                if (m % 2 == 1) y = ys.get(m/2);
                else            y = (ys.get(m/2 - 1) + ys.get(m/2)) / 2;
                outY.put(p, snap(y));
            } else {
                // Fallback: centro vertical del bloque
                outY.put(p, snap((minY + maxY) / 2));
            }
        }

        // 5) Instanciar INPUTS
        for (ModulePort p : inputs) {
            ComponentFactory pinFactory = Pin.FACTORY;
            AttributeSet attrs = pinFactory.createAttributeSet();

            attrs.setValue(StdAttr.WIDTH, BitWidth.create(Math.max(1, p.width())));
            attrs.setValue(Pin.ATTR_TYPE, false); // INPUT
            attrs.setValue(Pin.ATTR_TRISTATE, false);
            attrs.setValue(StdAttr.FACING, com.cburch.logisim.data.Direction.EAST); // mira hacia el bloque
            attrs.setValue(StdAttr.LABEL, p.name());

            Location loc = Location.create(snap(xInputs), snap(curInY));
            curInY += inStep;

            try {
                addComponentSafe(proj, circuit, g, pinFactory, loc, attrs);
            } catch (CircuitException e) {
                throw new IllegalStateException("No se pudo añadir pin input '" + p.name() + "'", e);
            }
        }

        // 6) Instanciar OUTPUTS (alineados a outY calculado)
        for (ModulePort p : outputs) {
            ComponentFactory pinFactory = Pin.FACTORY;
            AttributeSet attrs = pinFactory.createAttributeSet();

            attrs.setValue(StdAttr.WIDTH, BitWidth.create(Math.max(1, p.width())));
            attrs.setValue(Pin.ATTR_TYPE, true); // OUTPUT
            attrs.setValue(Pin.ATTR_TRISTATE, false);
            attrs.setValue(StdAttr.FACING, com.cburch.logisim.data.Direction.WEST); // mira hacia el bloque
            attrs.setValue(StdAttr.LABEL, p.name());

            int y = outY.getOrDefault(p, snap((minY + maxY) / 2));
            Location loc = Location.create(snap(xOutputs), y);

            try {
                addComponentSafe(proj, circuit, g, pinFactory, loc, attrs);
            } catch (CircuitException e) {
                throw new IllegalStateException("No se pudo añadir pin output '" + p.name() + "'", e);
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
            String bits = Arrays.stream(p.netIds())
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

            var topStrs  = new ArrayList<String>();
            var cellStrs = new ArrayList<String>();

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
