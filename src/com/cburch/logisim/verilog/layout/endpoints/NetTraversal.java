package com.cburch.logisim.verilog.layout.endpoints;


import com.cburch.logisim.verilog.layout.ModuleNetIndex;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Utility class for traversing all endpoints of a net.
 */
public final class NetTraversal {
    private NetTraversal() {}

    /** Devuelve todos los endpoints del net como lista inmutable de EndpointRef. */
    public static List<EndpointRef> list(ModuleNetIndex idx, int netId) {
        Objects.requireNonNull(idx, "idx");
        List<Integer> refs = idx.endpointsOf(netId);
        if (refs == null || refs.isEmpty()) return List.of();

        ArrayList<EndpointRef> out = new ArrayList<>(refs.size());
        for (int ref : refs) {
            if (ModuleNetIndex.isTop(ref)) out.add(new TopPortRef(ModuleNetIndex.ownerIdx(ref), ModuleNetIndex.bitIdx(ref)));
            else                           out.add(new CellPortRef(ModuleNetIndex.ownerIdx(ref), ModuleNetIndex.bitIdx(ref)));
        }
        return Collections.unmodifiableList(out);
    }

    /** Stream conveniente (no paralelo) de endpoints del net. */
    public static Stream<EndpointRef> stream(ModuleNetIndex idx, int netId) {
        return list(idx, netId).stream();
    }

    /** Aplica un consumidor a cada endpoint del net. */
    public static void forEach(ModuleNetIndex idx, int netId, Consumer<EndpointRef> consumer) {
        for (EndpointRef ep : list(idx, netId)) consumer.accept(ep);
    }

    /** Primer endpoint del net (o empty si no hay). Útil para elegir “fuente” al cablear. */
    public static Optional<EndpointRef> first(ModuleNetIndex idx, int netId) {
        List<EndpointRef> eps = list(idx, netId);
        return eps.isEmpty() ? Optional.empty() : Optional.of(eps.get(0));
    }
}


