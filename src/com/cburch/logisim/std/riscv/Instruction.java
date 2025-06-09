package com.cburch.logisim.std.riscv;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceState;

public class Instruction {
    int instruction;
    int opcode,d,s1,s2,func3;
    boolean imm,muldiv,func7;
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
        func3=((inst>>12)&0xf);
        func7=((inst>>30)&0x1)==1;
        s1=(inst>>15)&0x1f;
        s2=(inst>>20)&0x1f;
        imm=(inst==19);
        muldiv=((inst&0x7f)==48 && (inst>>25)==1);
    }
    public int getImmI(){
        int imm=(instruction>>20)&0xfff;
        if((imm >> 11) == 1){
            imm&=0xffffffff;
        }
        return imm;
    }
    public int getImmJ() {
        int imm = 0;
        imm += ((instruction >> 21) & 0xf) << 1;
        imm += ((instruction >> 25) & 0x3f) << 5;
        imm += ((instruction >> 20) & 0x1) << 11;
        imm += ((instruction >> 12) & 0xff) << 12;
        int sign = ((instruction >> 31) & 0x1);
        imm += sign << 20;
        if (sign == 1) {
            imm &= 0xffffffff;
        }
        return imm;
    }
}
