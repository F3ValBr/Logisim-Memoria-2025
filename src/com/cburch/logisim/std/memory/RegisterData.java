/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.instance.InstanceData;

class RegisterData extends ClockState implements InstanceData {
    int value;

    // índices de puertos (−1 si no aplica)
    int OUT = 0;
    int IN  = 1;
    int CK  = 2;
    int EN  = -1;
    int CLR = -1;

    public RegisterData() {
        value = 0;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
