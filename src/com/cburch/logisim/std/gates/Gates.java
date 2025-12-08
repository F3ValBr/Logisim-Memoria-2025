/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.gates;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Gates extends Library {

    public static final String LIB_NAME = "Gates";

    // ==== IDs públicos para cada componente ====
    public static final String NOT_ID              = NotGate._ID;
    public static final String BUFFER_ID           = Buffer._ID;
    public static final String AND_ID              = AndGate._ID;
    public static final String OR_ID               = OrGate._ID;
    public static final String NAND_ID             = NandGate._ID;
    public static final String NOR_ID              = NorGate._ID;
    public static final String XOR_ID              = XorGate._ID;
    public static final String XNOR_ID             = XnorGate._ID;
    public static final String ODD_PARITY_ID       = OddParityGate._ID;
    public static final String EVEN_PARITY_ID      = EvenParityGate._ID;
    public static final String CONTROLLED_BUFFER_ID    = ControlledBuffer.BUFFER_ID;
    public static final String CONTROLLED_INVERTER_ID  = ControlledBuffer.INVERTER_ID;

    private List<Tool> tools = null;

	public Gates() {
		tools = Arrays.asList(new Tool[] {
			new AddTool(NotGate.FACTORY),
			new AddTool(Buffer.FACTORY),
			new AddTool(AndGate.FACTORY),
			new AddTool(OrGate.FACTORY),
			new AddTool(NandGate.FACTORY),
			new AddTool(NorGate.FACTORY),
			new AddTool(XorGate.FACTORY),
			new AddTool(XnorGate.FACTORY),
			new AddTool(OddParityGate.FACTORY),
			new AddTool(EvenParityGate.FACTORY),
			new AddTool(ControlledBuffer.FACTORY_BUFFER),
			new AddTool(ControlledBuffer.FACTORY_INVERTER),
		});
	}

	@Override
	public String getName() { return LIB_NAME; }

	@Override
	public String getDisplayName() { return Strings.get("gatesLibrary"); }

	@Override
	public List<Tool> getTools() {
		return tools;
	}
}
