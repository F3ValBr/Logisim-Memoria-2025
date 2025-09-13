package com.cburch.logisim.verilog.layout.builder;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.auxiliary.ModulePort;
import com.cburch.logisim.verilog.layout.auxiliary.NodeSizer;
import com.cburch.logisim.verilog.layout.endpoints.NetTraversal;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleImpl;
import com.cburch.logisim.verilog.layout.endpoints.CellPortRef;
import com.cburch.logisim.verilog.layout.endpoints.EndpointRef;
import com.cburch.logisim.verilog.layout.ModuleNetIndex;
import com.cburch.logisim.verilog.layout.endpoints.TopPortRef;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.EdgeRouting;
import org.eclipse.elk.graph.*;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import org.eclipse.elk.core.options.CoreOptions;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public final class LayoutBuilder {
    public static class Result {
        public final ElkNode root;
        public final Map<VerilogCell, ElkNode> cellNode = new HashMap<>();
        public final Map<ModulePort, ElkNode>  portNode = new HashMap<>();
        public Result(ElkNode root){ this.root = root; }
    }

    // TODO: modificar esto para adaptar el layout
    public static Result build(Project proj,
                               VerilogModuleImpl mod,
                               ModuleNetIndex netIdx,
                               NodeSizer sizer) {
        ElkNode root = ElkGraphUtil.createGraph();
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(LayeredOptions.SPACING_NODE_NODE, 50.0);
        root.setProperty(CoreOptions.SPACING_COMPONENT_COMPONENT, 60.0);
        root.setProperty(LayeredOptions.EDGE_ROUTING, EdgeRouting.POLYLINE);
        root.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);

        // Inicio del proceso de layout
        Result r = new Result(root);

        // 1) Celdas
        for (VerilogCell cell : mod.cells()) {
            ElkNode n = ElkGraphUtil.createNode(root);

            java.awt.Dimension d = sizer.sizeForCell(proj, cell);
            n.setWidth(Math.max(30, d.width));
            n.setHeight(Math.max(20, d.height));

            ElkLabel lbl = ElkGraphUtil.createLabel(n);
            lbl.setText(cell.name());

            r.cellNode.put(cell, n);
        }

        // 2) Nodos para puertos top (opcionales, ayudan a anclar IO en los bordes)
        for (ModulePort p : mod.ports()) {
            ElkNode n = ElkGraphUtil.createNode(root);
            java.awt.Dimension d = sizer.sizeForTopPort(p);
            n.setWidth(d.width);
            n.setHeight(d.height);

            ElkLabel lbl = ElkGraphUtil.createLabel(n);
            lbl.setText(p.name());

            r.portNode.put(p, n);
        }

        // 3) Aristas: una por net
        for (int netId : netIdx.netIds()) {
            var refs = NetTraversal.list(netIdx, netId);
            LinkedHashSet<ElkNode> touched = new LinkedHashSet<>();
            for (EndpointRef ref : refs) {
                if (ref.isTop()) {
                    var t = (TopPortRef) ref;
                    ModulePort p = mod.ports().get(t.topPortIdx());
                    touched.add(r.portNode.get(p));
                } else {
                    var c = (CellPortRef) ref;
                    VerilogCell vc = mod.cells().get(c.cellIdx());
                    touched.add(r.cellNode.get(vc));
                }
            }
            // Conecta en estrella (fuente = primero)
            if (touched.size() >= 2) {
                ElkNode[] arr = touched.toArray(ElkNode[]::new);
                ElkNode src = arr[0];
                for (int i = 1; i < arr.length; i++) {
                    ElkEdge e = ElkGraphUtil.createSimpleEdge(src, arr[i]);
                    ElkLabel el = ElkGraphUtil.createLabel(e);
                    el.setText("n" + netId);
                }
            }
        }

        return r;
    }
}
