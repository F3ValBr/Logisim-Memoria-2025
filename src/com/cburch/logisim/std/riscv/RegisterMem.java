package com.cburch.logisim.std.riscv;

import com.cburch.logisim.instance.InstanceData;

public class RegisterMem implements InstanceData, Cloneable {
    private final int[] Register = new int[32];
    public RegisterMem() {}

    @Override
    public RegisterMem clone() {
        try {
            RegisterMem ret = (RegisterMem) super.clone();
            for(int i=0;i<Register.length;i++){ret.setValue(i,Register[i]);}
            return ret;
        } catch (CloneNotSupportedException e) { return null; }
    }
    public void setValue(int index, int value) {
        //can't change register 0
        if(index > 0 && index < Register.length) {
            Register[index] = value;
        }
    }
    public int getValue(int index) {
        if(index>0 && index < Register.length) {return Register[index];}
        return 0;}
}
