package com.cburch.logisim.verilog.comp.specs.wordlvl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum UnaryOp {
    BUF        ("$buf",         Category.BITWISE),
    LOGIC_NOT  ("$logic_not",   Category.LOGIC),
    NEG        ("$neg",         Category.ARITH),
    POS        ("$pos",         Category.ARITH),
    NOT        ("$not",         Category.BITWISE),
    REDUCE_AND ("$reduce_and",  Category.REDUCE),
    REDUCE_BOOL("$reduce_bool", Category.REDUCE),
    REDUCE_OR  ("$reduce_or",   Category.REDUCE),
    REDUCE_XNOR("$reduce_xnor", Category.REDUCE),
    REDUCE_XOR ("$reduce_xor",  Category.REDUCE);

    public enum Category { BITWISE, LOGIC, ARITH, REDUCE }

    private final String yosysId;
    private final Category category;

    UnaryOp(String yosysId, Category category) {
        this.yosysId = yosysId;
        this.category = category;
    }

    public String yosysId() { return yosysId; }
    public Category category() { return category; }

    public boolean isReduce()  { return category == Category.REDUCE; }
    public boolean isLogic()   { return category == Category.LOGIC; }
    public boolean isBitwise() { return category == Category.BITWISE; }
    public boolean isArith()   { return category == Category.ARITH; }

    // ------- Índice estático para resolución rápida -------
    private static final Map<String, UnaryOp> INDEX;
    static {
        Map<String, UnaryOp> m = new HashMap<>();
        for (UnaryOp op : values()) m.put(op.yosysId, op);
        INDEX = Collections.unmodifiableMap(m);
    }

    /** Verifies if the given typeId corresponds to a known UnaryOp
     * @param typeId The type identifier to check
     * @return true if the typeId corresponds to a known UnaryOp, false otherwise
     */
    public static boolean isUnaryTypeId(String typeId) {
        return INDEX.containsKey(typeId);
    }

    /** Yosys typeId to UnaryOp conversion (throws if unknown)
     *
     * @param typeId The type identifier to convert
     * @return The corresponding UnaryOp
     * @throws IllegalArgumentException if the typeId is unknown
     */
    public static UnaryOp fromYosys(String typeId) {
        UnaryOp op = INDEX.get(typeId);
        if (op == null) throw new IllegalArgumentException("Unknown unary op: " + typeId);
        return op;
    }

    /** Secure version of fromYosys (returns Optional.empty() if unknown)
     *
     * @param typeId The type identifier to convert
     * @return An Optional containing the corresponding UnaryOp, or Optional.empty() if unknown
     */
    public static Optional<UnaryOp> tryFromYosys(String typeId) {
        return Optional.ofNullable(INDEX.get(typeId));
    }
}

