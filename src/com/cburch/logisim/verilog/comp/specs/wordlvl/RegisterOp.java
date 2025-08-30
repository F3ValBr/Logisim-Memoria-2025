package com.cburch.logisim.verilog.comp.specs.wordlvl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum RegisterOp {
    ADFF    ("$adff"),
    ADFFE   ("$adffe"),
    ADLATCH ("$adlatch"),
    ALDFF   ("$aldff"),
    ALDFFE  ("$aldffe"),
    DFF     ("$dff"),
    DFFE    ("$dffe"),
    DFFSR   ("$dffsr"),
    DFFSRE  ("$dffsre"),
    DLATCH  ("$dlatch"),
    DLATCHSR("$dlatchsr"),
    SDFF    ("$sdff"),
    SDFFCE  ("$sdffce"),
    SDFFE   ("$sdffe"),
    SR      ("$sr");

    private final String yosysId;
    RegisterOp(String id) { this.yosysId = id; }
    public String yosysId() { return yosysId; }

    private static final Map<String, RegisterOp> INDEX;
    static {
        var m = new HashMap<String, RegisterOp>();
        for (var op : values()) m.put(op.yosysId, op);
        INDEX = Collections.unmodifiableMap(m);
    }
    public static boolean isRegisterTypeId(String typeId) { return INDEX.containsKey(typeId); }

    public static RegisterOp fromYosys(String typeId) {
        var op = INDEX.get(typeId);
        if (op == null) throw new IllegalArgumentException("Unknown RegisterOp typeId: " + typeId);
        return op;
    }

    public static Optional<RegisterOp> tryFromYosys(String typeId) {
        return Optional.ofNullable(INDEX.get(typeId));
    }
}
