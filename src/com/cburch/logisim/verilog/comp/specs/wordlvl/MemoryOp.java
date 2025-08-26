package com.cburch.logisim.verilog.comp.specs.wordlvl;

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

    private static final java.util.Map<String, MemoryOp> INDEX;
    static {
        var m = new java.util.HashMap<String, MemoryOp>();
        for (var op : values()) m.put(op.yosysId, op);
        INDEX = java.util.Collections.unmodifiableMap(m);
    }
    public static boolean isTypeId(String typeId) { return INDEX.containsKey(typeId); }
    public static java.util.Optional<MemoryOp> tryFromYosys(String typeId) {
        return java.util.Optional.ofNullable(INDEX.get(typeId));
    }
}
