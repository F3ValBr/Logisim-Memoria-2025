package com.cburch.logisim.std.riscv;

import com.cburch.logisim.data.Value;

public class Instruction {
    int instruction;
    int opcode,d,s1,s2,func3;
    boolean imm, mulDiv,func7;
    public Instruction(int inst){
        setValues(inst);
    }
    public Instruction(Value inst){
        instruction = inst.toIntValue();
        setValues(instruction);
    }
    private void setValues(int inst){
        instruction = inst;
        opcode=inst&0x7f;
        d=(inst>>7)&0x1f;
        func3=((inst>>12)&0x7);
        func7=((inst>>30)&0x1)==1;
        s1=(inst>>15)&0x1f;
        s2=(inst>>20)&0x1f;
        imm=(opcode==0x13);
        mulDiv =((inst&0x7f)==0x33 && ((inst>>25)&1)==1);
    }
    public int getImm() {
        return switch (opcode) {
            case 0b1101111 -> getImmJ();
            // I-type
            case 0b1100111, 0b0000011, 0b0010011 -> (instruction >> 20); //31-20
            // S-type
            case 0b0100011 -> (((instruction >> 20) & 0xFFFFFFE0) |
                            ((instruction >>> 7) & 0x0000001F));
            // B-type
            case 0b1100011 -> getImmB();
            // U-type
            case 0b0110111, 0b0010111 -> instruction & 0xFFFFF000;
            default -> 0;
        };
    }

    private int getImmB() {
        int imm=0;
        imm|=(instruction>>7)&0x1E;//1-4
        imm+=(instruction>>20)&0x7E0;//5-10
        imm+=(instruction<<4)&0x800;//11
        imm+=(instruction>>19)&0x1000;//12
        if ((imm & 0x1000) != 0) {
            imm |= 0xFFFFE000;
        }
        return imm;
    }

    private int getImmJ() {
        int imm = 0;
        imm += ((instruction >> 20) & 0x7FE);//1-10
        imm += ((instruction >> 9) & 0x800);//11
        imm += (instruction & 0xFF000);//12-19
        imm += (instruction >>31)<<20;
        return imm;
    }
}
