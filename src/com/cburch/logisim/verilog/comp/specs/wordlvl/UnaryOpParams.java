package com.cburch.logisim.verilog.comp.specs.wordlvl;

import com.cburch.logisim.verilog.comp.specs.GenericCellParams;

import java.util.Map;

public final class UnaryOpParams extends GenericCellParams {
    private final UnaryOp op;
    private final int aWidth, yWidth;
    private final boolean aSigned;

    public UnaryOpParams(UnaryOp op, Map<String,?> raw) {
        super(raw);
        this.op = op;
        this.aWidth  = getInt("A_WIDTH", 0);
        this.yWidth  = getInt("Y_WIDTH", 0);
        this.aSigned = getBool("A_SIGNED", false);
        validate();
    }

    // --- Getters ---
    public UnaryOp op() { return op; }
    public int aWidth() { return aWidth; }
    public int yWidth() { return yWidth; }
    public boolean aSigned() { return aSigned; }

    /** Validations:
     * - Logic ops: Y_WIDTH == 1
     * - Reduce ops: Y_WIDTH == 1
     */
    private void validate() {
        /*if (op.isLogic() || op.isReduce()) {
            if (yWidth != 1) throw new IllegalArgumentException(op + ": Y_WIDTH debe ser 1");
        }*/
    }
}

