package com.cburch.logisim.verilog.file.jsonhdlr;

import java.util.List;
import java.util.Map;

public record YosysCellDTO(
        String name,
        String typeId,
        Map<String,String> parameters,
        Map<String,Object> attributes,
        Map<String,String> portDirections,
        Map<String, List<Object>> connections
) {}

