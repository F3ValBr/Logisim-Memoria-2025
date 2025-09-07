package com.cburch.logisim.verilog.comp.auxiliary;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class FactoryLookup {
    public static ComponentFactory findFactory(Library lib, String displayName) {
        if (lib == null) return null;
        for (Tool t : lib.getTools()) {
            if (t instanceof AddTool add) {
                ComponentFactory f = add.getFactory();
                if (displayName.equals(f.getName())) {
                    return f;
                }
            }
        }
        return null;
    }
}
