package com.cburch.logisim.verilog.comp.aux;

import java.util.*;

public class Net {
    private final int id;
    private final String name;
    private final List<PortEndpoint> endpoints;
    private final Map<String, Object> attributes;

    public Net(int id, String name) {
        this.id = id;
        this.name = name;
        this.endpoints = new ArrayList<>();
        this.attributes = new HashMap<>();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<PortEndpoint> getEndpoints() { return Collections.unmodifiableList(endpoints); }
    public Map<String, Object> getAttributes() { return attributes; }

    public void addEndpoint(PortEndpoint endpoint) {
        endpoints.add(endpoint);
    }

    public void addAttribute(String key, Object value) {
        attributes.put(key, value);
    }
}

