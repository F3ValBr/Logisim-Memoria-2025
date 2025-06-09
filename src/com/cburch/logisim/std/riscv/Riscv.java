package com.cburch.logisim.std.riscv;

import com.cburch.logisim.std.memory.Memory;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

import java.util.List;

public class Riscv extends Library {
    private static final FactoryDescription[] DESCRIPTIONS = {
            new FactoryDescription(("processor"),Strings.getter("processorComponent"),
                    "rom.gif","Processor")
    };
    private List<Tool> tools = null;

    public Riscv(){ }
    @Override
    public String getName() { return "Risc-V"; }

    @Override
    public String getDisplayName() { return Strings.get("riscVLibrary"); }

    @Override
    public List<Tool> getTools() {
        if (tools == null) {
            tools = FactoryDescription.getTools(Riscv.class, DESCRIPTIONS);
        }
        return tools;
    }
}
