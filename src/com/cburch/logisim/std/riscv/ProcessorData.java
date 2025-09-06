package com.cburch.logisim.std.riscv;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

import java.awt.*;

public class ProcessorData implements InstanceData,Cloneable, HexModelListener {
    public Integer programCount;
    public Integer intrProgramCount;
    public Instruction instruction;
    public RegisterMem register;
    public Integer opcode;
    public Value lastClock;
    public Boolean low;
    public Boolean intr_enable;
    public Boolean ls_PC;
    DataContents contents;
    BitWidth BITWIDTH=BitWidth.create(32);
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
        intr_enable=false;
        ls_PC=false;
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
        programCount=(getS1()+offset)&0xFFFFFFFE;
    }
    public Integer getS1() {return register.getValue(instruction.s1);}
    public Integer getS2() {return register.getValue(instruction.s2);}
    public DataContents getContents() {return contents;}
    @Override
    public ProcessorData clone() {
        try {
            ProcessorData ret= (ProcessorData) super.clone();
            ret.programCount=programCount;
            ret.intrProgramCount=intrProgramCount;
            ret.instruction=new Instruction(instruction.instruction);
            ret.register=register.clone();
            ret.opcode=opcode;
            ret.lastClock=lastClock;
            ret.low=low;
            ret.intr_enable=intr_enable;
            ret.ls_PC=ls_PC;
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
        int columns;
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
        if (cursorLoc > newLast) cursorLoc = newLast;
        if (curAddr - newLast > 0) curAddr = -1;
        long maxScroll = Math.max(0, newLast + 1 - (ROWS - 1) * columns);
        if (curScroll > maxScroll) curScroll = maxScroll;
    }
    private static final int ENTRY_HEIGHT = 18;

    public void paint(Graphics g, int leftX, int topY) {
        int boxX = leftX+4;
        int boxY = topY+38;
        int boxW = 292;
        int boxH = 8 * (ENTRY_HEIGHT);
        g.drawRect(boxX, boxY, boxW, boxH);
        g.drawLine(boxX+22, boxY,
                boxX+22, boxY+boxH);
        for(int col=1;col<4;col++){
            g.drawLine(boxX+(boxW*col/4), boxY,
                    boxX+(boxW*col/4), boxY+boxH);
            g.drawLine(boxX+(boxW*col/4)+22, boxY,
                    boxX+(boxW*col/4)+22, boxY+boxH);
        }
        int entryWidth = boxW / 4;
        g.setColor(Color.BLACK);
        for (int row = 0; row < 8; row++) {
            int y = boxY + ENTRY_HEIGHT * row;
            g.drawLine(boxX, y,
                    boxX+boxW, y);
            for (int col = 0; col < 4; col++) {
                int x = boxX+col*entryWidth;
                g.setColor(Color.GRAY);
                GraphicsUtil.drawText(g, ("X"+(4*row + col)),
                                x+12, y+ENTRY_HEIGHT/2,
                                GraphicsUtil.H_CENTER, GraphicsUtil.H_CENTER);
                int val = register.getValue(4*row + col);
                g.setColor(Color.BLACK);
                GraphicsUtil.drawText(g, StringUtil.toHexString(32, val),
                        x+24, y+ENTRY_HEIGHT/2,
                        GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);

            }
        }
        boxY+=boxH+30;
        g.drawRect(boxX, boxY, boxW/3, boxH/8);
        g.drawLine(boxX+28, boxY,boxX+28, boxY+boxH/8);
        g.setColor(Color.GRAY);
        GraphicsUtil.drawText(g, ("INST"),
                boxX+14, boxY+ENTRY_HEIGHT/2,
                GraphicsUtil.H_CENTER, GraphicsUtil.H_CENTER);
        g.setColor(Color.BLACK);
        GraphicsUtil.drawText(g, StringUtil.toHexString(32, instruction.instruction),
                boxX+boxW/6+14, boxY+ENTRY_HEIGHT/2,
                GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
        boxX=leftX+2*boxW/3;
        g.drawRect(boxX, boxY, boxW/3, boxH/8);
        g.drawLine(boxX+28, boxY,boxX+28, boxY+boxH/8);
        g.setColor(Color.GRAY);
        GraphicsUtil.drawText(g, ("PC"),
                boxX+14, boxY+ENTRY_HEIGHT/2,
                GraphicsUtil.H_CENTER, GraphicsUtil.H_CENTER);
        g.setColor(Color.BLACK);
        GraphicsUtil.drawText(g, StringUtil.toHexString(32, programCount),
                boxX+boxW/6+14, boxY+ENTRY_HEIGHT/2,
                GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
    }

    @Override
    public void metainfoChanged(HexModel source) {

    }

    @Override
    public void bytesChanged(HexModel source, long start, long numBytes, int[] oldValues) {

    }
    public void intrPC(int intrAddr){
        register.setValue(2,register.getValue(2)-4);
        setInstruction(0x00012023); //SW modified store PC
        ls_PC=true;
        intr_enable=false;
        intrProgramCount=programCount;
        programCount=intrAddr;
    }
    public void loadPC(int pc) {
        programCount=pc;
        ls_PC =false;
    }
    public void retPC(){
        register.setValue(2,register.getValue(2)+4);
        setInstruction(0xffc12003);
        ls_PC=true;
        //System.out.println(register.getValue(2));
    }
}
