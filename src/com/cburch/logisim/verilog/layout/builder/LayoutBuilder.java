package com.cburch.logisim.verilog.layout.builder;

import com.cburch.logisim.verilog.comp.auxiliary.ModulePort;
import com.cburch.logisim.verilog.layout.endpoints.NetTraversal;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.impl.VerilogModuleImpl;
import com.cburch.logisim.verilog.layout.endpoints.CellPortRef;
import com.cburch.logisim.verilog.layout.endpoints.EndpointRef;
import com.cburch.logisim.verilog.layout.ModuleNetIndex;
import com.cburch.logisim.verilog.layout.endpoints.TopPortRef;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.graph.*;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import org.eclipse.elk.core.options.CoreOptions;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;


public final class LayoutBuilder {
    public static class Result {
        public final ElkNode root;
        // mapas para volver a ubicar
        public final Map<VerilogCell, ElkNode> cellNode = new HashMap<>();
        public final Map<ModulePort, ElkNode> topNode   = new HashMap<>();
        public Result(ElkNode root){ this.root = root; }
    }

    public static Result build(VerilogModuleImpl mod, ModuleNetIndex netIdx) {
        ElkNode root = ElkGraphUtil.createGraph();
        // opciones típicas del alg. layered
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(LayeredOptions.SPACING_NODE_NODE, 50.0);
        root.setProperty(CoreOptions.DIRECTION, org.eclipse.elk.core.options.Direction.RIGHT);
        root.setProperty(LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY, LayeredOptions.CONSIDER_MODEL_ORDER_STRATEGY.getDefault());

        // Inicio del proceso de layout
        Result r = new Result(root);

        // Primer ciclo de Nodos: uno por celda
        for (VerilogCell cell : mod.cells()) {
            ElkNode n = ElkGraphUtil.createNode(root);
            // tamaño sugerido (si aún no instanciaste la caja en Logisim, usa heurística)
            double w = 50, h = 50; // fallback
            // si ya tienes SubcircuitFactory, puedes consultar bounds (offset) para hacer mejor hint
            n.setWidth(w);
            n.setHeight(h);
            // etiqueta (opcional)
            ElkLabel lbl = ElkGraphUtil.createLabel(n);
            lbl.setText(cell.name());
            r.cellNode.put(cell, n);
        }

        // 2) Nodos para puertos top (opcionales, ayudan a anclar IO en los bordes)
        for (ModulePort p : mod.ports()) {
            ElkNode n = ElkGraphUtil.createNode(root);
            n.setWidth(20);
            n.setHeight(20);
            ElkLabel lbl = ElkGraphUtil.createLabel(n);
            lbl.setText(p.name());
            // fija el lado (opcional): entradas a la izquierda, salidas a la derecha
            // no hay API directa para "pin to border" sin comp, pero layered tenderá a ubicarlos en extremos si los conectas bien.
            r.topNode.put(p, n);
        }

        // 3) Aristas: una por net
        for (int netId : netIdx.netIds()) {
            var eps = NetTraversal.list(netIdx, netId);

            // Junta nodos ELK tocados por la net
            LinkedHashSet<ElkNode> touched = new LinkedHashSet<>();
            for (EndpointRef ep : eps) {
                if (ep.isTop()) {
                    TopPortRef t = (TopPortRef) ep;
                    ModulePort p = mod.ports().get(t.topPortIdx());
                    touched.add(r.topNode.get(p));
                } else {
                    CellPortRef c = (CellPortRef) ep;
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
