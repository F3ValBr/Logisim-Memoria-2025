package com.cburch.logisim.verilog.file.jsonhdlr;

import java.util.Map;

public record YosysMemoryDTO(
        String memId,
        int width,
        int size,
        int startOffset,
        Map<String,Object> attributes
) {}