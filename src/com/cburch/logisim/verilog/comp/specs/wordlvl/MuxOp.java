package com.cburch.logisim.verilog.comp.specs.wordlvl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum MuxOp {
    BMUX    ("$bmux"),
    BWMUX   ("$bwmux"),
    DEMUX   ("$demux"),
    MUX     ("$mux"),
    PMUX    ("$pmux"),
    TRIBUF  ("$tribuf");

    private final String yosysId;
    MuxOp(String id) { this.yosysId = id; }
    public String yosysId() { return yosysId; }

    private static final Map<String, MuxOp> INDEX;
    static {
        var m = new HashMap<String, MuxOp>();
        for (var op : values()) m.put(op.yosysId, op);
        INDEX = Collections.unmodifiableMap(m);
    }

    public static boolean isMuxTypeId(String typeId) {
        return INDEX.containsKey(typeId);
    }

    public static MuxOp fromYosys(String typeId) {
        var op = INDEX.get(typeId);
        if (op == null) throw new IllegalArgumentException("Unknown MuxOp typeId: " + typeId);
        return op;
    }

    public static Optional<MuxOp> tryFromYosys(String typeId) {
        return Optional.ofNullable(INDEX.get(typeId));
    }
}
