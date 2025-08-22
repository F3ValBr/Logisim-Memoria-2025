package com.cburch.logisim.verilog.comp.specs.gatelvl;

public enum GateOp {
    AND, BUF, MUX, NAND, NOR, NOT, OR, XNOR, XOR,
    ANDNOT, AOI3, AOI4, MUX16, MUX4, MUX8, NMUX, OAI3, OAI4, ORNOT;

    public static GateOp fromYosys(String typeId) {
        if (typeId == null) throw new IllegalArgumentException("typeId null");

        return switch (typeId) {
            // Simple
            case "$_AND_"  -> AND;
            case "$_BUF_"  -> BUF;
            case "$_MUX_"  -> MUX;
            case "$_NAND_" -> NAND;
            case "$_NOR_"  -> NOR;
            case "$_NOT_"  -> NOT;
            case "$_OR_"   -> OR;
            case "$_XNOR_" -> XNOR;
            case "$_XOR_"  -> XOR;
            // Combined
            case "$_ANDNOT_" -> ANDNOT;
            case "$_AOI3_"   -> AOI3;
            case "$_AOI4_"   -> AOI4;
            case "$_MUX16_"  -> MUX16;
            case "$_MUX4_"   -> MUX4;
            case "$_MUX8_"   -> MUX8;
            case "$_NMUX_"   -> NMUX;
            case "$_OAI3_"   -> OAI3;
            case "$_OAI4_"   -> OAI4;
            case "$_ORNOT_"  -> ORNOT;
            default -> throw new IllegalArgumentException("Unknown gate op: " + typeId);
        };
    }
}

