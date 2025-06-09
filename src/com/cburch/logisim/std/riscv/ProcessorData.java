package com.cburch.logisim.std.riscv;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;

public class ProcessorData implements InstanceData,Cloneable, HexModelListener {
    public Integer programCount;
    public Instruction instruction;
    public RegisterMem register;
    public Integer opcode;
    public Value lastClock;
    public Boolean low;
    public DataContents contents;
    BitWidth BITWIDTH=BitWidth.create(32);
    private int columns;
    private long curScroll = 0;
    private long cursorLoc = -1;
    private long curAddr = -1;
    private static final int ROWS = 4;


    ProcessorData(DataContents contents,int boot) {
        this.contents = contents.clone();
        setBits(contents.getLogLength(), contents.getWidth());
        contents.addHexModelListener(this);
        programCount = boot;
        instruction = new Instruction(Value.createKnown(BITWIDTH, 0));
        register = new RegisterMem();
        opcode = 0;
        lastClock = Value.UNKNOWN;
        low=false;
    }
    public void setInstruction(int inst) {
        instruction=new Instruction(inst);
    }
    public void setInstruction(Value inst) {
        instruction=new Instruction(inst);
    }
    public void stepPC(Integer offset){
        programCount+=offset;
    }
    public void stepPCS1(Integer offset){
        programCount=getS1()+offset;
    }
    public Integer getS1() {return register.getValue(instruction.s1);}
    public Integer getS2() {return register.getValue(instruction.s2);}
    @Override
    public ProcessorData clone() {
        try {
            ProcessorData ret= (ProcessorData) super.clone();
            ret.contents=contents.clone();
            ret.contents.addHexModelListener(ret);
            return ret;
        } catch (CloneNotSupportedException e) { return null; }
    }
    private void setBits(int addrBits, int dataBits) {
        if (contents == null) {
            contents = DataContents.create(addrBits, dataBits);
        } else {
            contents.setDimensions(addrBits, dataBits);
        }
        if (addrBits <= 12) {
            if (dataBits <= 8) {
                columns = dataBits <= 4 ? 8 : 4;
            } else {
                columns = dataBits <= 16 ? 2 : 1;
            }
        } else {
            columns = dataBits <= 8 ? 2 : 1;
        }
        long newLast = contents.getLastOffset();
        // I do subtraction in the next two conditions to account for possibility of overflow
        if (cursorLoc > newLast) cursorLoc = newLast;
        if (curAddr - newLast > 0) curAddr = -1;
        long maxScroll = Math.max(0, newLast + 1 - (ROWS - 1) * columns);
        if (curScroll > maxScroll) curScroll = maxScroll;
    }
    @Override
    public void metainfoChanged(HexModel source) {

    }

    @Override
    public void bytesChanged(HexModel source, long start, long numBytes, int[] oldValues) {

    }

}
