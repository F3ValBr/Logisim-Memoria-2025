package com.cburch.logisim.verilog.comp.specs.wordlvl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum BinaryOp {
    // Bitwise
    AND      ("$and",       Category.BITWISE),
    OR       ("$or",        Category.BITWISE),
    XOR      ("$xor",       Category.BITWISE),
    XNOR     ("$xnor",      Category.BITWISE),

    // Lógicas / comparadores
    LOGIC_AND("$logic_and", Category.LOGIC),
    LOGIC_OR ("$logic_or",  Category.LOGIC),
    EQ       ("$eq",        Category.COMPARE),
    NE       ("$ne",        Category.COMPARE),
    LT       ("$lt",        Category.COMPARE),
    LE       ("$le",        Category.COMPARE),
    GT       ("$gt",        Category.COMPARE),
    GE       ("$ge",        Category.COMPARE),
    EQX      ("$eqx",       Category.COMPARE),  // igualdad con X/Z
    NEX      ("$nex",       Category.COMPARE),  // desigualdad con X/Z

    // Aritméticos
    ADD      ("$add",       Category.ARITH),
    SUB      ("$sub",       Category.ARITH),
    MUL      ("$mul",       Category.ARITH),
    DIV      ("$div",       Category.ARITH),
    DIVFLOOR ("$divfloor",  Category.ARITH),
    MOD      ("$mod",       Category.ARITH),
    MODFLOOR ("$modfloor",  Category.ARITH),
    POW      ("$pow",       Category.ARITH),

    // Shifts
    SHL      ("$shl",       Category.SHIFT),
    SHR      ("$shr",       Category.SHIFT),
    SSHL     ("$sshl",      Category.SHIFT),
    SSHR     ("$sshr",      Category.SHIFT),
    SHIFT    ("$shift",     Category.SHIFT),    // shift genérico
    SHIFTX   ("$shiftx",    Category.SHIFT),    // shift con relleno X

    // Misceláneo
    BWEQX    ("$bweqx",     Category.MISC);

    public enum Category { BITWISE, LOGIC, COMPARE, ARITH, SHIFT, MISC }

    private final String yosysId;
    private final Category category;

    BinaryOp(String yosysId, Category category) {
        this.yosysId = yosysId;
        this.category = category;
    }

    public String yosysId() { return yosysId; }
    public Category category() { return category; }

    public boolean isBitwise() { return category == Category.BITWISE; }
    public boolean isLogic()   { return category == Category.LOGIC; }
    public boolean isCompare() { return category == Category.COMPARE; }
    public boolean isArith()   { return category == Category.ARITH; }
    public boolean isShift()   { return category == Category.SHIFT; }

    // ------- Índice estático -------
    private static final Map<String, BinaryOp> INDEX;
    static {
        Map<String, BinaryOp> m = new HashMap<>();
        for (BinaryOp op : values()) m.put(op.yosysId, op);
        INDEX = Collections.unmodifiableMap(m);
    }

    /** Verifies if the given typeId corresponds to a known BinaryOp
     * @param typeId The type identifier to check
     * @return true if the typeId corresponds to a known BinaryOp, false otherwise
     */
    public static boolean isBinaryTypeId(String typeId) {
        return INDEX.containsKey(typeId);
    }

    /** Yosys typeId to BinaryOp conversion (throws if unknown)
     *
     * @param typeId The type identifier to convert
     * @return The corresponding BinaryOp
     * @throws IllegalArgumentException if the typeId is unknown
     */
    public static BinaryOp fromYosys(String typeId) {
        BinaryOp op = INDEX.get(typeId);
        if (op == null) throw new IllegalArgumentException("Unknown binary op: " + typeId);
        return op;
    }

    /** Secure version of fromYosys (returns Optional.empty() if unknown)
     *
     * @param typeId The type identifier to convert
     * @return An Optional containing the corresponding BinaryOp, or Optional.empty() if unknown
     */
    public static Optional<BinaryOp> tryFromYosys(String typeId) {
        return Optional.ofNullable(INDEX.get(typeId));
    }
}

