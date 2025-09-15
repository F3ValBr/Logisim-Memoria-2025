package com.cburch.logisim.verilog.layout.builder;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.auxiliary.ModulePort;
import com.cburch.logisim.verilog.layout.auxiliary.NodeSizer;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleImpl;
import com.cburch.logisim.verilog.layout.ModuleNetIndex;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
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
        public final ElkNode root;
        public final Map<VerilogCell, ElkNode> cellNode = new HashMap<>();
        public final Map<ModulePort, ElkNode>  portNode = new HashMap<>();
        public Result(ElkNode root){ this.root = root; }
    }

    // --- Agrupadores/keys para buses -----------------------------------------

    /** Identifica un “extremo lógico” por (nodo, nombre-de-puerto) para no mezclar buses distintos por el mismo par de nodos. */
    private static final class EpKey {
        final ElkNode node;
        final String portName; // puede ser null
        EpKey(ElkNode node, String portName) {
            this.node = node;
            this.portName = portName;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EpKey)) return false;
            EpKey k = (EpKey) o;
            return node == k.node && Objects.equals(portName, k.portName);
        }
        @Override public int hashCode() {
            return 31 * System.identityHashCode(node) + Objects.hashCode(portName);
        }
    }

    /** Par dirigido (src,dst) + baseLabel (nombre lógico del bus para la etiqueta). */
    private static final class PairKey {
        final EpKey src;
        final EpKey dst;
        final String baseLabel; // p.ej. "data", "A→Y", "n123"
        PairKey(EpKey src, EpKey dst, String baseLabel) {
            this.src = src; this.dst = dst; this.baseLabel = baseLabel;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PairKey)) return false;
            PairKey k = (PairKey) o;
            return Objects.equals(src, k.src) &&
                    Objects.equals(dst, k.dst) &&
                    Objects.equals(baseLabel, k.baseLabel);
        }
        @Override public int hashCode() {
            return (31 * src.hashCode() + dst.hashCode()) * 31 + Objects.hashCode(baseLabel);
        }
    }

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

    private static final class RefInfo {
        final ElkNode node;
        final String portName; // puede ser null
        final int bitIndex;
        RefInfo(ElkNode node, String portName, int bitIndex) {
            this.node = node; this.portName = portName; this.bitIndex = bitIndex;
        }
    }

    // -------------------------------------------------------------------------

    public static Result build(Project proj,
                               VerilogModuleImpl mod,
                               ModuleNetIndex netIdx,
                               NodeSizer sizer) {
        // --- Grafo raíz y opciones ELK ---
        ElkNode root = ElkGraphUtil.createGraph();
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(LayeredOptions.SPACING_NODE_NODE, 50.0);
        root.setProperty(CoreOptions.SPACING_COMPONENT_COMPONENT, 60.0);
        root.setProperty(LayeredOptions.EDGE_ROUTING, EdgeRouting.POLYLINE);
        root.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);
        root.setProperty(LayeredOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.CENTER);

        Result r = new Result(root);

        // --- 1) Celdas internas como nodos ---
        for (VerilogCell cell : mod.cells()) {
            ElkNode n = ElkGraphUtil.createNode(root);

            Dimension d = (sizer != null)
                    ? sizer.sizeForCell(proj, cell)
                    : new Dimension(60, 60);

            n.setWidth(Math.max(30, d.width));
            n.setHeight(Math.max(20, d.height));

            ElkLabel lbl = ElkGraphUtil.createLabel(n);
            lbl.setText(cell.name());

            r.cellNode.put(cell, n);
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
        }

        // --- 3) Aristas: agrupar bits por (src,dst,baseLabel) para crear UNA arista por bus ---

        // (src,dst,baseLabel) -> conjunto de índices de bit
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
                    ElkNode node = r.portNode.get(p);
                    String pname = p.name();
                    infos.add(new RefInfo(node, pname, bit));
                } else {
                    int cIdx = ModuleNetIndex.ownerIdx(ref);
                    VerilogCell vc = mod.cells().get(cIdx);
                    ElkNode node = r.cellNode.get(vc);
                    String pname = netIdx.resolveCellPortName(ref).orElse(null);
                    infos.add(new RefInfo(node, pname, bit));
                }
            }

            // Estrella estable desde el primero
            RefInfo src = infos.get(0);
            for (int i = 1; i < infos.size(); i++) {
                RefInfo dst = infos.get(i);

                String base = chooseBaseLabel(src.portName, dst.portName, netId);
                PairKey key = new PairKey(new EpKey(src.node, src.portName),
                        new EpKey(dst.node, dst.portName),
                        base);

                SortedSet<Integer> set = busGroups.computeIfAbsent(key, k -> new TreeSet<>());
                set.add(src.bitIndex);
                set.add(dst.bitIndex);
            }

            /*
             * ——— Si más adelante quieres orientación DRIVER→SINK ———
             * Puedes reemplazar el bloque anterior por:
             * - Detectar roles de PortEndpoint / ModulePort (IN/OUT/INOUT)
             * - Conectar DRIVER -> (SINK ∪ INOUT)
             * - Fallback a estrella si no hay drivers claros
             * Mantuve esta versión “agnóstica de dirección” para que compile
             * en tu Logisim sin depender de tipos concretos.
             */
        }

        // Crear UNA arista por grupo y etiquetar con rangos de bits
        for (Map.Entry<PairKey, SortedSet<Integer>> e : busGroups.entrySet()) {
            PairKey k = e.getKey();
            ElkEdge edge = ElkGraphUtil.createSimpleEdge(k.src.node, k.dst.node);

            String idxs = compactRanges(e.getValue());
            String label = (k.baseLabel == null || k.baseLabel.isBlank())
                    ? ("bus [" + idxs + "]")
                    : (k.baseLabel + " [" + idxs + "]");

            ElkLabel el = ElkGraphUtil.createLabel(edge);
            el.setText(label);

            // (Opcional) Guardar ancho para trazo más grueso en tu renderer:
            // int width = e.getValue().size();
            // edge.setProperty(CoreOptions.COMMENT, "busWidth=" + width);
        }

        return r;
    }
}
