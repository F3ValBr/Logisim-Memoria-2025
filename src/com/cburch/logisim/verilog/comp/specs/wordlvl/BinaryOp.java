package com.cburch.logisim.verilog.comp.specs.wordlvl;

public enum BinaryOp {
    ADD, AND, BWEQX, DIV, DIVFLOOR, EQ, EQX, GE, GT, LE, LOGIC_AND, LOGIC_OR, LT,
    MOD, MODFLOOR, MUL, NE, NEX, OR, POW, SHIFT, SHIFTX, SHL, SHR, SSHL, SSHR, SUB, XNOR, XOR;
    public static BinaryOp fromYosys(String typeId) {
        if (typeId == null) throw new IllegalArgumentException("typeId null");

        return switch (typeId) {
            case "$add"       -> ADD;
            case "$and"       -> AND;
            case "$bweqx"     -> BWEQX;
            case "$div"       -> DIV;
            case "$divfloor"  -> DIVFLOOR;
            case "$eq"        -> EQ;
            case "$eqx"       -> EQX;
            case "$ge"        -> GE;
            case "$gt"        -> GT;
            case "$le"        -> LE;
            case "$logic_and" -> LOGIC_AND;
            case "$logic_or"  -> LOGIC_OR;
            case "$lt"        -> LT;
            case "$mod"       -> MOD;
            case "$modfloor"  -> MODFLOOR;
            case "$mul"       -> MUL;
            case "$ne"        -> NE;
            case "$nex"       -> NEX;
            case "$or"        -> OR;
            case "$pow"       -> POW;
            case "$shift"     -> SHIFT;
            case "$shiftx"    -> SHIFTX;
            case "$shl"       -> SHL;
            case "$shr"       -> SHR;
            case "$sshl"      -> SSHL;
            case "$sshr"      -> SSHR;
            case "$sub"       -> SUB;
            case "$xnor"      -> XNOR;
            case "$xor"       -> XOR;
            default -> throw new IllegalArgumentException("Unknown binary op: " + typeId);
        };
    }
    public boolean isLogic()   { return this==EQ||this==NE||this==LT||this==LE||this==GT||this==GE||this==LOGIC_AND||this==LOGIC_OR; }
    public boolean isBitwise() { return this==AND||this==OR||this==XOR||this==XNOR; }
    public boolean isArith()   { return this==ADD||this==SUB||this==MUL||this==DIV||this==MOD||this==POW; }
    public boolean isShift()   { return this==SHL||this==SHR||this==SSHL||this==SSHR||this==SHIFT||this==SHIFTX; }
}
