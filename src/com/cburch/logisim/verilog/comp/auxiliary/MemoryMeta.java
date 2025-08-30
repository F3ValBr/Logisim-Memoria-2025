package com.cburch.logisim.verilog.comp.auxiliary;

import java.util.Map;

public record MemoryMeta(Integer width, Integer size, Integer startOffset, Map<String,Object> attributes) {}
