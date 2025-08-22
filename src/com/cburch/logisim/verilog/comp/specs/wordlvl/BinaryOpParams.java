package com.cburch.logisim.verilog.comp.specs.wordlvl;

import com.cburch.logisim.verilog.comp.specs.GenericCellParams;

import java.util.Map;

public final class BinaryOpParams extends GenericCellParams {
    private final BinaryOp op;
    private final int aWidth, bWidth, yWidth;
    private final boolean aSigned, bSigned;

    public BinaryOpParams(BinaryOp op, Map<String,?> raw) {
        super(raw);
        this.op = op;
        this.aWidth = getInt("A_WIDTH", 0);
        this.bWidth = getInt("B_WIDTH", 0);
        this.yWidth = getInt("Y_WIDTH", 0);
        this.aSigned = getBool("A_SIGNED", false);
        this.bSigned = getBool("B_SIGNED", false);
        validate();
    }

    public BinaryOp op() { return op; }
    public int aWidth() { return aWidth; }
    public int bWidth() { return bWidth; }
    public int yWidth() { return yWidth; }
    public boolean aSigned() { return aSigned; }
    public boolean bSigned() { return bSigned; }

    private void validate() {
        if (op.isLogic()) {
            if (yWidth != 1) throw new IllegalArgumentException(op + ": Y_WIDTH debe ser 1");
        } else if (op.isShift()) {
            if (yWidth != aWidth) throw new IllegalArgumentException(op + ": Y_WIDTH debe == A_WIDTH");
            // B es amount; no fuerzas bWidth aquí (puede ser >= log2(A))
        } else if (op.isBitwise()) {
            if (yWidth != Math.max(aWidth, bWidth))
                throw new IllegalArgumentException(op + ": Y_WIDTH debe == max(A_WIDTH, B_WIDTH)");
        } else if (op.isArith()) {
            // Política simple; ajusta según reglas que definas
            if (yWidth < Math.max(aWidth, bWidth))
                throw new IllegalArgumentException(op + ": Y_WIDTH debe >= max(A_WIDTH, B_WIDTH)");
        }
    }
}

