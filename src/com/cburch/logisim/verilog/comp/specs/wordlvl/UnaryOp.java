package com.cburch.logisim.verilog.comp.specs.wordlvl;

public enum UnaryOp {
    BUF, LOGIC_NOT, NEG, NOT, POS, REDUCE_AND, REDUCE_BOOL, REDUCE_OR, REDUCE_XNOR, REDUCE_XOR;
    public static UnaryOp fromYosys(String typeId) {
        if (typeId == null) throw new IllegalArgumentException("typeId null");

        return switch (typeId) {
            case "$buf"        -> BUF;
            case "$logic_not"  -> LOGIC_NOT;
            case "$neg"        -> NEG;
            case "$not"        -> NOT;
            case "$pos"        -> POS;
            case "$reduce_and" -> REDUCE_AND;
            case "$reduce_bool"-> REDUCE_BOOL;
            case "$reduce_or"  -> REDUCE_OR;
            case "$reduce_xnor"-> REDUCE_XNOR;
            case "$reduce_xor" -> REDUCE_XOR;
            default -> throw new IllegalArgumentException("Unknown unary op: " + typeId);
        };
    }
    public boolean isReduce()   { return name().startsWith("REDUCE_"); }
    public boolean isLogic()    { return this == LOGIC_NOT; }
    public boolean isBitwise()  { return this == BUF || this == NOT; }
    public boolean isArith()    { return this == NEG || this == POS; }
}
