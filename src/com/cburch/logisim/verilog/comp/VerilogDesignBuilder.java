package com.cburch.logisim.verilog.comp;

import com.cburch.logisim.verilog.file.jsonhdlr.YosysJsonNetlist;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysModuleDTO;

import java.util.*;

// TODO: implementar con eventual manejo de logisim
public final class VerilogDesignBuilder {
    private final VerilogModuleBuilder moduleBuilder;

    public VerilogDesignBuilder(CellFactoryRegistry registry) {
        this.moduleBuilder = new VerilogModuleBuilder(registry);
    }

    /** Construye todos los módulos sin ordenar. */
    public Map<String, VerilogModuleImpl> buildAllUnordered(YosysJsonNetlist netlist) {
        Map<String, VerilogModuleImpl> out = new LinkedHashMap<>();
        netlist.modules().forEach(dto -> out.put(dto.name(), moduleBuilder.buildModule(dto)));
        return out;
    }

    /** Construye todos los módulos respetando dependencias (submódulos → padres). */
    public Map<String, VerilogModuleImpl> buildAllTopologically(YosysJsonNetlist netlist) {
        // 1) Armar grafo módulo → submódulos referenciados
        Map<String, Set<String>> deps = computeModuleDeps(netlist);

        // 2) Orden topológico (Kahn)
        List<String> order = topoOrder(deps);

        // 3) Construir en ese orden
        Map<String, VerilogModuleImpl> out = new LinkedHashMap<>();
        for (String m : order) {
            YosysModuleDTO dto = netlist.getModule(m)
                    .orElseThrow(() -> new IllegalStateException("Module missing: " + m));
            out.put(m, moduleBuilder.buildModule(dto));
        }
        return out;
    }

    /** Dependencias: para cada módulo, qué módulos instancian (cells sin prefijo '$'). */
    private Map<String, Set<String>> computeModuleDeps(YosysJsonNetlist netlist) {
        Set<String> mods = netlist.moduleNames();
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        for (String m : mods) {
            Set<String> submods = new LinkedHashSet<>();
            netlist.getModule(m).ifPresent(dto -> dto.cells().forEach(c -> {
                String typeId = c.typeId();
                if (!typeId.startsWith("$")) {            // módulo de usuario (no celda primitiva)
                    if (mods.contains(typeId)) submods.add(typeId);
                }
            }));
            deps.put(m, submods);
        }
        return deps;
    }

    /** Orden topológico simple (Kahn). */
    private List<String> topoOrder(Map<String, Set<String>> deps) {
        Map<String, Integer> indeg = new LinkedHashMap<>();
        deps.forEach((m, ss) -> indeg.put(m, 0));
        deps.forEach((m, ss) -> ss.forEach(s -> indeg.put(s, indeg.get(s) + 1)));

        Deque<String> q = new ArrayDeque<>();
        indeg.forEach((m, d) -> { if (d == 0) q.add(m); });

        List<String> order = new ArrayList<>(deps.size());
        while (!q.isEmpty()) {
            String u = q.removeFirst();
            order.add(u);
            for (String v : deps.getOrDefault(u, Set.of())) {
                int d = indeg.merge(v, -1, Integer::sum);
                if (d == 0) q.add(v);
            }
        }
        if (order.size() != deps.size()) {
            // Hay ciclo (poco común en módulos HW). Igual devuelve algo útil:
            // añade los restantes en cualquier orden.
            for (String m : deps.keySet()) if (!order.contains(m)) order.add(m);
        }
        return order;
    }

    public VerilogModuleBuilder moduleBuilder() { return moduleBuilder; }
}
