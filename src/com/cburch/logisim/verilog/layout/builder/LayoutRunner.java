package com.cburch.logisim.verilog.layout.builder;

import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.util.NullElkProgressMonitor;
import org.eclipse.elk.graph.ElkNode;

/**
 * Ejecuta el layout en un grafo ELK.
 */
public final class LayoutRunner {
    private static final RecursiveGraphLayoutEngine ENGINE = new RecursiveGraphLayoutEngine();

    /**
     * Ejecuta el layout en el grafo ELK dado.
     *
     * @param root El nodo raíz del grafo a layoutar.
     */
    public static void run(ElkNode root) {
        ENGINE.layout(root, new NullElkProgressMonitor());
    }
}

