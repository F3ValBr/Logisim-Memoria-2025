package com.cburch.logisim.verilog.layout;

import org.eclipse.elk.alg.layered.LayeredLayoutProvider;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.core.util.NullElkProgressMonitor;

/**
 * Utilidades para aplicar layout y ajustar posiciones.
 */
public class LayoutUtils {

    /**
     * Aplica un layout al nodo raíz y ajusta las posiciones de los nodos hijos
     * para que todas las coordenadas X e Y sean al menos minX y minY.
     *
     * @param root El nodo raíz del grafo a layoutar.
     * @param minX La coordenada mínima X deseada.
     * @param minY La coordenada mínima Y deseada.
     */
    public static void applyLayoutAndClamp(ElkNode root, int minX, int minY) {
        // Ejecutar el layout
        new LayeredLayoutProvider().layout(root, new NullElkProgressMonitor());

        // Buscar los mínimos globales
        double minCoordX = Double.MAX_VALUE;
        double minCoordY = Double.MAX_VALUE;

        // Recorrer hijos y actualizar mínimos
        for (ElkNode child : root.getChildren()) {
            minCoordX = Math.min(minCoordX, child.getX());
            minCoordY = Math.min(minCoordY, child.getY());
        }

        // Calcular offset para que todos queden >= minX, minY
        double offsetX = (minCoordX < minX) ? (minX - minCoordX) : 0;
        double offsetY = (minCoordY < minY) ? (minY - minCoordY) : 0;

        // Aplicar desplazamiento a cada nodo
        if (offsetX != 0 || offsetY != 0) {
            for (ElkNode child : root.getChildren()) {
                child.setX(child.getX() + offsetX);
                child.setY(child.getY() + offsetY);
            }
        }
    }
}

