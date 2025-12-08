package com.cburch.logisim.std.riscv;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

import java.util.List;

public class Riscv extends Library {

    public static final String LIB_NAME = "Risc-V";

    // ==== IDs públicos de procesadores ====
    public static final String RV32IM_ID       = RV32IM._ID;
    public static final String RV32IM_MICRO_ID = RV32IMMicro._ID;

    // ==== FactoryDescription renovados ====
    private static final FactoryDescription[] DESCRIPTIONS = {
        new FactoryDescription(RV32IM_ID,
            Strings.getter("processorRV32IM"),
            "riscvproc.gif",
            RV32IM.class.getSimpleName()
        ),
        new FactoryDescription(RV32IM_MICRO_ID,
            Strings.getter("processorRV32IMMicro"),
            "riscvprocmicro.gif",
            RV32IMMicro.class.getSimpleName()
        ),
    };

    private List<Tool> tools = null;

    public Riscv(){ }
    @Override
    public String getName() { return LIB_NAME; }

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
