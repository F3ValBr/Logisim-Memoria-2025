package com.cburch.logisim.verilog.layout.builder;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.auxiliary.ModulePort;
import com.cburch.logisim.verilog.comp.auxiliary.netconn.PortDirection;
import com.cburch.logisim.verilog.comp.impl.AbstractVerilogCell;
import com.cburch.logisim.verilog.layout.auxiliary.NodeSizer;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleImpl;
import com.cburch.logisim.verilog.layout.ModuleNetIndex;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.EdgeLabelPlacement;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.graph.*;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import org.eclipse.elk.core.options.CoreOptions;

import java.awt.*;
import java.util.*;
import java.util.List;

public final class LayoutBuilder {

    public static class Result {
        public ElkNode root;
        // Nodos ELK
        public final Map<VerilogCell, ElkNode> cellNode = new HashMap<>();
        public final Map<ModulePort, ElkNode>  portNode = new HashMap<>();
        // Puertos ELK
        public final Map<CellPortKey, ElkPort> cellPorts = new HashMap<>();
        public final Map<ModulePort, ElkPort>  topPorts  = new HashMap<>();

        public Result(ElkNode root){ this.root = root; }
    }

    public record CellPortKey(VerilogCell cell, String portName) {}

    // --- Agrupadores/keys para buses -----------------------------------------

    /**
     * Identifica un “extremo lógico” por (puerto, nombre-de-puerto) para no mezclar buses distintos por el mismo par de nodos.
     */
    private record EpKey(ElkPort port, String portName) {
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EpKey k)) return false;
            return port == k.port && Objects.equals(portName, k.portName);
        }

        @Override public int hashCode() {
            return 31 * System.identityHashCode(port) + Objects.hashCode(portName);
        }
    }

    /**
     * Par dirigido (src,dst) + baseLabel (nombre lógico del bus para la etiqueta).
     */
    private record PairKey(EpKey src, EpKey dst, String baseLabel) {
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PairKey k)) return false;
            return Objects.equals(src, k.src) &&
                    Objects.equals(dst, k.dst) &&
                    Objects.equals(baseLabel, k.baseLabel);
        }

        @Override public int hashCode() {
            return (31 * src.hashCode() + dst.hashCode()) * 31 + Objects.hashCode(baseLabel);
        }
    }

    private record RefInfo(ElkPort port, String portName, int bitIndex) { }

    // --- Utilidades -----------------------------------------------------------

    /** Compacta índices de bit a "0,2-5,7". */
    private static String compactRanges(SortedSet<Integer> idxs) {
        if (idxs == null || idxs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        Integer start = null, prev = null;
        for (Integer x : idxs) {
            if (start == null) { start = prev = x; continue; }
            if (x == prev + 1) { prev = x; continue; }
            if (start.equals(prev)) sb.append(start);
            else sb.append(start).append("-").append(prev);
            sb.append(",");
            start = prev = x;
        }
        if (start.equals(prev)) sb.append(start);
        else sb.append(start).append("-").append(prev);
        return sb.toString();
    }

    /** Elige una etiqueta base para el bus a partir de nombres de puertos. */
    private static String chooseBaseLabel(String a, String b, int netId) {
        if (a != null && b != null) return a.equals(b) ? a : (a + "→" + b);
        if (a != null) return a;
        if (b != null) return b;
        return "n" + netId;
    }

    private static PortDirection dirOf(VerilogCell cell, String portName) {
        if (cell instanceof AbstractVerilogCell ac) return ac.getPortDirection(portName);
        return PortDirection.UNKNOWN;
    }

    private static void addCellElkPorts(Result r, VerilogCell cell, ElkNode n) {
        List<String> ins = new ArrayList<>();
        List<String> outs = new ArrayList<>();
        List<String> inouts = new ArrayList<>();
        List<String> other = new ArrayList<>();

        for (String pname : cell.getPortNames()) {
            PortDirection d = dirOf(cell, pname);
            if (d == PortDirection.INPUT) ins.add(pname);
            else if (d == PortDirection.OUTPUT) outs.add(pname);
            else if (d == PortDirection.INOUT) inouts.add(pname);
            else other.add(pname);
        }

        ins.sort(String::compareTo);
        outs.sort(String::compareTo);
        inouts.sort(String::compareTo);
        other.sort(String::compareTo);

        // Con Direction.RIGHT:
        for (String pname : ins)   mkCellPort(r, cell, pname, n, org.eclipse.elk.core.options.PortSide.WEST);
        for (String pname : outs)  mkCellPort(r, cell, pname, n, org.eclipse.elk.core.options.PortSide.EAST);

        // INOUT/unknown a SOUTH (cámbialo si quieres)
        for (String pname : inouts) mkCellPort(r, cell, pname, n, org.eclipse.elk.core.options.PortSide.SOUTH);
        for (String pname : other)  mkCellPort(r, cell, pname, n, org.eclipse.elk.core.options.PortSide.SOUTH);
    }

    private static void mkCellPort(Result r, VerilogCell cell, String pname, ElkNode n,
                                   org.eclipse.elk.core.options.PortSide side) {
        ElkPort p = ElkGraphUtil.createPort(n);
        p.setIdentifier(pname);
        p.setWidth(6);
        p.setHeight(6);
        p.setProperty(CoreOptions.PORT_SIDE, side);

        r.cellPorts.put(new CellPortKey(cell, pname), p);
    }

    private static void addTopElkPort(Result r, ModulePort mp, ElkNode n) {
        ElkPort p = ElkGraphUtil.createPort(n);
        p.setIdentifier(mp.name());
        p.setWidth(6);
        p.setHeight(6);

        // Direction.RIGHT:
        // INPUT del módulo -> hacia interior => EAST
        // OUTPUT del módulo -> desde interior => WEST
        var side = (mp.direction() == PortDirection.INPUT)
                ? org.eclipse.elk.core.options.PortSide.EAST
                : org.eclipse.elk.core.options.PortSide.WEST;

        p.setProperty(CoreOptions.PORT_SIDE, side);
        r.topPorts.put(mp, p);
    }

    /* ========== BUILDER ========== */

    public static Result build(Project proj,
                               VerilogModuleImpl mod,
                               ModuleNetIndex netIdx,
                               NodeSizer sizer,
                               Map<VerilogCell, VerilogCell> cellAlias) {
        // --- Grafo raíz y opciones ELK ---
        ElkNode root = ElkGraphUtil.createGraph();
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");

        // --- separaciones principales ---
        // distancia entre capas
        root.setProperty(LayeredOptions.SPACING_EDGE_EDGE_BETWEEN_LAYERS, 40.0);
        root.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, 60.0);

        // distancia entre nodos de la misma capa (vertical)
        root.setProperty(LayeredOptions.SPACING_NODE_NODE, 60.0);

        // margen global entre “componentes” sueltos
        root.setProperty(CoreOptions.SPACING_COMPONENT_COMPONENT, 40.0);

        // si hay puertos o labels en el medio, a veces ayuda esto:
        root.setProperty(CoreOptions.SPACING_LABEL_NODE, 20.0);
        root.setProperty(CoreOptions.SPACING_PORT_PORT, 20.0);

        // dirección
        root.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);
        root.setProperty(LayeredOptions.EDGE_ROUTING, EdgeRouting.POLYLINE);
        root.setProperty(LayeredOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.CENTER);

        // padding del diagrama entero
        root.setProperty(CoreOptions.PADDING, new ElkPadding(20, 20, 20, 20));

        root.setProperty(CoreOptions.PORT_CONSTRAINTS, org.eclipse.elk.core.options.PortConstraints.FIXED_SIDE);

        root.setProperty(CoreOptions.PORT_CONSTRAINTS, org.eclipse.elk.core.options.PortConstraints.FIXED_ORDER);

        Result r = new Result(root);

        // --- 1) Celdas internas como nodos ---
        for (VerilogCell cell : mod.cells()) {
            // si es alias, NO creamos nodo; su representante tendrá el nodo
            if (cellAlias.containsKey(cell)) continue;

            ElkNode n = ElkGraphUtil.createNode(root);

            Dimension d = (sizer != null)
                    ? sizer.sizeForCell(proj, cell)
                    : new Dimension(60, 60);

            n.setWidth(Math.max(30, d.width));
            n.setHeight(Math.max(20, d.height));

            ElkLabel lbl = ElkGraphUtil.createLabel(n);
            lbl.setText(cell.name());

            r.cellNode.put(cell, n);
            addCellElkPorts(r, cell, n);
        }

        // --- 2) Puertos top como nodos ---
        for (ModulePort p : mod.ports()) {
            ElkNode n = ElkGraphUtil.createNode(root);

            Dimension d = (sizer != null)
                    ? sizer.sizeForTopPort(p)
                    : new Dimension(20, 20);

            n.setWidth(Math.max(20, d.width));
            n.setHeight(Math.max(20, d.height));

            ElkLabel lbl = ElkGraphUtil.createLabel(n);
            lbl.setText(p.name());

            r.portNode.put(p, n);
            addTopElkPort(r, p, n);
        }

        // --- 3) Aristas agrupadas por bus (src,dst,baseLabel) ---
        Map<PairKey, SortedSet<Integer>> busGroups = new HashMap<>();

        for (int netId : netIdx.netIds()) {
            List<Integer> refs = netIdx.endpointsOf(netId);
            if (refs == null || refs.size() < 2) continue;

            // Resolvemos cada endpoint a (nodo ELK, nombre de puerto, bit)
            List<RefInfo> infos = new ArrayList<>(refs.size());
            for (int ref : refs) {
                int bit = ModuleNetIndex.bitIdx(ref);

                if (ModuleNetIndex.isTop(ref)) {
                    int pIdx = netIdx.resolveTopPortIdx(ref);
                    ModulePort p = mod.ports().get(pIdx);

                    ElkPort port = r.topPorts.get(p);

                    infos.add(new RefInfo(port, p.name(), bit));
                } else {
                    int cIdx = ModuleNetIndex.ownerIdx(ref);
                    VerilogCell owner = mod.cells().get(cIdx);
                    // Remapear al representante si es alias
                    VerilogCell repr = cellAlias.getOrDefault(owner, owner);

                    String pname = netIdx.resolveCellPortName(ref).orElse(null);

                    ElkPort port = (pname == null) ? null : r.cellPorts.get(new CellPortKey(repr, pname));

                    // fallback (evita perder arista)
                    if (port == null) {
                        ElkNode node = r.cellNode.get(repr);
                        if (node == null) {
                            node = ElkGraphUtil.createNode(root);
                            Dimension d = (sizer != null) ? sizer.sizeForCell(proj, repr) : new Dimension(60, 60);
                            node.setWidth(Math.max(30, d.width));
                            node.setHeight(Math.max(20, d.height));
                            ElkLabel lbl = ElkGraphUtil.createLabel(node);
                            lbl.setText(repr.name());
                            r.cellNode.put(repr, node);
                            addCellElkPorts(r, repr, node);
                        }

                        ElkPort fp = ElkGraphUtil.createPort(node);
                        fp.setIdentifier(pname != null ? pname : ("p" + ref));
                        fp.setWidth(4);
                        fp.setHeight(4);
                        fp.setProperty(CoreOptions.PORT_SIDE, org.eclipse.elk.core.options.PortSide.SOUTH);

                        port = fp;
                        if (pname != null) r.cellPorts.put(new CellPortKey(repr, pname), port);
                    }

                    infos.add(new RefInfo(port, pname, bit));
                }
            }

            // Estrella estable desde el primero
            RefInfo src = infos.get(0);
            for (int i = 1; i < infos.size(); i++) {
                RefInfo dst = infos.get(i);

                String base = chooseBaseLabel(src.portName, dst.portName, netId);

                PairKey key = new PairKey(
                        new EpKey(src.port, src.portName),
                        new EpKey(dst.port, dst.portName),
                        base
                );

                SortedSet<Integer> set = busGroups.computeIfAbsent(key, k -> new TreeSet<>());
                set.add(src.bitIndex);
                set.add(dst.bitIndex);
            }
        }

        // Crear UNA arista por grupo y etiquetar con rangos de bits
        for (Map.Entry<PairKey, SortedSet<Integer>> e : busGroups.entrySet()) {
            PairKey k = e.getKey();
            ElkEdge edge = ElkGraphUtil.createEdge(root);
            edge.getSources().add(k.src.port());
            edge.getTargets().add(k.dst.port());

            String idxs = compactRanges(e.getValue());
            String label = (k.baseLabel == null || k.baseLabel.isBlank())
                    ? ("bus [" + idxs + "]")
                    : (k.baseLabel + " [" + idxs + "]");

            ElkLabel el = ElkGraphUtil.createLabel(edge);
            el.setText(label);
        }

        return r;
    }
}
