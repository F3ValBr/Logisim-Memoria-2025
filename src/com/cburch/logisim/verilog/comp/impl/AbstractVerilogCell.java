package com.cburch.logisim.verilog.comp.impl;

import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.PortEndpoint;
import com.cburch.logisim.verilog.comp.specs.CellAttribs;
import com.cburch.logisim.verilog.comp.specs.CellParams;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractVerilogCell implements VerilogCell {
    protected String name;
    protected CellType cellType;
    protected CellParams params;
    protected CellAttribs attribs;
    protected List<PortEndpoint> endpoints = new ArrayList<>();

    protected AbstractVerilogCell(String name, CellType type, CellParams params, CellAttribs attribs) {
        this.name = name;
        this.cellType = type;
        this.params = params;
        this.attribs = attribs;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CellType type() {
        return cellType;
    }

    @Override
    public CellParams params() {
        return params;
    }

    @Override
    public CellAttribs attribs() {
        return attribs;
    }

    @Override
    public List<PortEndpoint> endpoints() {
        return endpoints;
    }

    @Override
    public int portWidth(String portName) {
        if (portName == null || portName.isEmpty()) {
            throw new IllegalArgumentException("Port name cannot be null or empty");
        }
        return (int) endpoints.stream()
                .filter(endpoint -> endpoint.getPortName().equals(portName))
                .count();
    }

    @Override
    public List<String> getPortNames() {
        Set<String> portNames = new LinkedHashSet<>();
        for (PortEndpoint endpoint : endpoints) {
            portNames.add(endpoint.getPortName());
        }
        return new ArrayList<>(portNames);
    }

    @Override
    public void addPortEndpoint(PortEndpoint endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("PortEndpoint cannot be null");
        }
        endpoints.add(endpoint);
    }

    @Override
    public String typeId() {
        return cellType != null ? cellType.typeId() : null;
    }

    @Override
    public String kind() {
        return cellType != null ? cellType.kind().toString() : null;
    }

    @Override
    public String level() {
        return cellType != null ? cellType.level().toString() : null;
    }
}
