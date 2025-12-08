/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import java.util.List;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Memory extends Library {

    public static final String LIB_NAME = "Memory";

    protected static final int DELAY = 5;

    // ==== IDs públicos para Sequential / Memory ====
    public static final String DFF_ID            = DFlipFlop._ID;
    public static final String TFF_ID            = TFlipFlop._ID;
    public static final String JKFF_ID           = JKFlipFlop._ID;
    public static final String SRFF_ID           = SRFlipFlop._ID;
    public static final String REGISTER_ID       = Register._ID;
    public static final String COUNTER_ID        = Counter._ID;
    public static final String SHIFTREG_ID       = ShiftRegister._ID;
    public static final String RANDOM_ID         = Random._ID;
    public static final String RAM_ID            = Ram._ID;
    public static final String ROM_ID            = Rom._ID;
    public static final String TIMER_ID          = Timer._ID;

    // ==== Descriptions ====
    private static final FactoryDescription[] DESCRIPTIONS = {
        new FactoryDescription(DFF_ID,
            Strings.getter("dFlipFlopComponent"),
            "dFlipFlop.gif",
            DFlipFlop.class.getSimpleName()
        ),
        new FactoryDescription(TFF_ID,
            Strings.getter("tFlipFlopComponent"),
            "tFlipFlop.gif",
            TFlipFlop.class.getSimpleName()
        ),
        new FactoryDescription(JKFF_ID,
            Strings.getter("jkFlipFlopComponent"),
            "jkFlipFlop.gif",
            JKFlipFlop.class.getSimpleName()
        ),
        new FactoryDescription(SRFF_ID,
            Strings.getter("srFlipFlopComponent"),
            "srFlipFlop.gif",
            SRFlipFlop.class.getSimpleName()
        ),
        new FactoryDescription(REGISTER_ID,
            Strings.getter("registerComponent"),
            "register.gif",
            Register.class.getSimpleName()
        ),
        new FactoryDescription(COUNTER_ID,
            Strings.getter("counterComponent"),
            "counter.gif",
            Counter.class.getSimpleName()
        ),
        new FactoryDescription(SHIFTREG_ID,
            Strings.getter("shiftRegisterComponent"),
            "shiftreg.gif",
            ShiftRegister.class.getSimpleName()
        ),
        new FactoryDescription(RANDOM_ID,
            Strings.getter("randomComponent"),
            "random.gif",
            Random.class.getSimpleName()
        ),
        new FactoryDescription(RAM_ID,
            Strings.getter("ramComponent"),
            "ram.gif",
            Ram.class.getSimpleName()
        ),
        new FactoryDescription(ROM_ID,
            Strings.getter("romComponent"),
            "rom.gif",
            Rom.class.getSimpleName()
        ),
        new FactoryDescription(TIMER_ID,
            Strings.getter("timerComponent"),
            "timer.gif",
            Timer.class.getSimpleName()
        ),
    };

    private List<Tool> tools = null;

	public Memory() { }

	@Override
	public String getName() { return LIB_NAME; }

	@Override
	public String getDisplayName() { return Strings.get("memoryLibrary"); }

	@Override
	public List<Tool> getTools() {
		if (tools == null) {
			tools = FactoryDescription.getTools(Memory.class, DESCRIPTIONS);
		}
		return tools;
	}
}
