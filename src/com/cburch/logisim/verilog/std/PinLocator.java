package com.cburch.logisim.verilog.std;

import com.cburch.logisim.data.Location;

public interface PinLocator {
    Location locate(String portName, int bitIndex);
}