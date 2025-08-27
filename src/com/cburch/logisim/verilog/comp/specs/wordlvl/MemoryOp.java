package com.cburch.logisim.verilog.comp.specs.wordlvl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum MemoryOp {
    MEM         ("$mem"),
    MEM_V2      ("$mem_v2"),
    MEMINIT     ("$meminit"),
    MEMINIT_V2  ("$meminit_v2"),
    MEMRD       ("$memrd"),
    MEMRD_V2    ("$memrd_v2"),
    MEMWR       ("$memwr"),
    MEMWR_V2    ("$memwr_v2");

    private final String yosysId;
    MemoryOp(String id) { this.yosysId = id; }
    public String yosysId() { return yosysId; }

    private static final Map<String, MemoryOp> INDEX;
    static {
        var m = new HashMap<String, MemoryOp>();
        for (var op : values()) m.put(op.yosysId, op);
        INDEX = Collections.unmodifiableMap(m);
    }
    public static boolean isMemoryTypeId(String typeId) { return INDEX.containsKey(typeId); }

    public static MemoryOp fromYosys(String typeId) {
        var op = INDEX.get(typeId);
        if (op == null) throw new IllegalArgumentException("Unknown MemoryOp typeId: " + typeId);
        return op;
    }

    public static Optional<MemoryOp> tryFromYosys(String typeId) {
        return Optional.ofNullable(INDEX.get(typeId));
    }
}
