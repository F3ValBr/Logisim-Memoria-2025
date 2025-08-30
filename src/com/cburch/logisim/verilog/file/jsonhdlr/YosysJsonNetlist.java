package com.cburch.logisim.verilog.file.jsonhdlr;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class YosysJsonNetlist {
    private final JsonNode root;
    private final JsonNode modules;

    private YosysJsonNetlist(JsonNode root, JsonNode modules) {
        this.root = root;
        this.modules = modules;
    }

    /**
     * Creates a YosysJsonNetlist from the root JSON node of a Yosys JSON dump.
     * Validates that the root and modules nodes are present and of the correct type.
     * @param root Root JSON node of the Yosys dump.
     * @return YosysJsonNetlist instance.
     */
    public static YosysJsonNetlist from(JsonNode root) {
        if (root == null || root.isMissingNode())
            throw new IllegalArgumentException("Root JSON is null/missing");
        JsonNode mods = root.path("modules");
        if (!mods.isObject())
            throw new IllegalArgumentException("Root JSON has no 'modules' object");
        return new YosysJsonNetlist(root, mods);
    }

    /**
     * Returns the name of the top module, if specified.
     * @return Optional containing the top module name, or empty if not specified.
     */
    public Optional<String> topModule() {
        JsonNode t = root.get("top");
        if (t != null && t.isTextual()) return Optional.of(t.asText());
        // algunos dumps guardan top en attributes:design_top, ajusta si necesitas
        return Optional.empty();
    }


    public Set<String> moduleNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        modules.fieldNames().forEachRemaining(names::add);
        return names;
    }

    public Optional<YosysModuleDTO> getModule(String name) {
        JsonNode n = modules.get(name);
        return (n != null && n.isObject()) ? Optional.of(new YosysModuleDTO(name, n)) : Optional.empty();
    }

    public Stream<YosysModuleDTO> modules() {
        Iterable<Map.Entry<String,JsonNode>> it = modules::fields;
        return StreamSupport.stream(it.spliterator(), false)
                .filter(e -> e.getValue().isObject())
                .map(e -> new YosysModuleDTO(e.getKey(), e.getValue()));
    }
}

