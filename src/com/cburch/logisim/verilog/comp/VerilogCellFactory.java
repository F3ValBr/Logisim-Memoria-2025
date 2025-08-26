package com.cburch.logisim.verilog.comp;

import java.util.List;
import java.util.Map;

public interface VerilogCellFactory {
    /**
     * Creates a new Verilog cell with the specified name, type, ports, parameters, attributes, and connections.
     *
     * @param name the name of the cell
     * @param type the type of the cell
     * @param ports a map of port names to their types
     * @param params a map of parameter names to their values
     * @param attribs a map of attribute names to their values
     * @param connections a map of port names to their connections
     *                  where each connection is a list of objects representing the endpoints
     *                  (e.g., wires, other cells, etc.)
     * @return a new VerilogCell instance
     */
    VerilogCell create(
        String name,
        String type,
        Map<String, String> params,
        Map<String, Object> attribs,
        Map<String, String> ports,
        Map<String, List<Object>> connections
    );
}
