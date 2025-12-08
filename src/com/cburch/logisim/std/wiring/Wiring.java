/* Copyright (c) 2011, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.wiring;

import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Wiring extends Library {

    public static final String LIB_NAME = "Wiring";

	static final AttributeOption GATE_TOP_LEFT
		= new AttributeOption("tl", Strings.getter("wiringGateTopLeftOption"));
	static final AttributeOption GATE_BOTTOM_RIGHT
		= new AttributeOption("br", Strings.getter("wiringGateBottomRightOption"));
	static final Attribute<AttributeOption> ATTR_GATE = Attributes.forOption("gate",
			Strings.getter("wiringGateAttr"),
			new AttributeOption[] { GATE_TOP_LEFT, GATE_BOTTOM_RIGHT });

	private static Tool[] ADD_TOOLS = {
		new AddTool(SplitterFactory.instance),
		new AddTool(Pin.FACTORY),
		new AddTool(Probe.FACTORY),
		new AddTool(Tunnel.FACTORY),
        new AddTool(BitLabeledTunnel.FACTORY),
        new AddTool(PullResistor.FACTORY),
        new AddTool(Clock.FACTORY),
        new AddTool(Constant.FACTORY),
    };

    // ==== IDs públicos de componentes Wiring ====
    public static final String SPLITTER_ID            = SplitterFactory._ID;
    public static final String PIN_ID                 = Pin._ID;
    public static final String PROBE_ID               = Probe._ID;
    public static final String TUNNEL_ID              = Tunnel._ID;
    public static final String BIT_LABELED_TUNNEL_ID  = BitLabeledTunnel._ID;
    public static final String PULL_RESISTOR_ID       = PullResistor._ID;
    public static final String CLOCK_ID               = Clock._ID;
    public static final String CONSTANT_ID            = Constant._ID;
    public static final String POWER_ID               = Power._ID;
    public static final String GROUND_ID              = Ground._ID;
    public static final String TRANSISTOR_ID          = Transistor._ID;
    public static final String TRANSMISSION_GATE_ID   = TransmissionGate._ID;
    public static final String BIT_EXTENDER_ID        = BitExtender._ID;

    private static final FactoryDescription[] DESCRIPTIONS = {
        new FactoryDescription(POWER_ID,
            Strings.getter("powerComponent"),
            "power.gif",
            Power.class.getSimpleName()
        ),
        new FactoryDescription(GROUND_ID,
            Strings.getter("groundComponent"),
            "ground.gif",
            Ground.class.getSimpleName()
        ),
        new FactoryDescription(TRANSISTOR_ID,
            Strings.getter("transistorComponent"),
            "trans0.gif",
            Transistor.class.getSimpleName()
        ),
        new FactoryDescription(TRANSMISSION_GATE_ID,
            Strings.getter("transmissionGateComponent"),
            "transmis.gif",
            TransmissionGate.class.getSimpleName()
        ),
        new FactoryDescription(BIT_EXTENDER_ID,
            Strings.getter("extenderComponent"),
            "extender.gif",
            BitExtender.class.getSimpleName()
        ),
    };

    private List<Tool> tools = null;

	public Wiring() { }

	@Override
	public String getName() { return LIB_NAME; }

	@Override
	public String getDisplayName() { return Strings.get("wiringLibrary"); }

	@Override
	public List<Tool> getTools() {
		if (tools == null) {
			List<Tool> ret = new ArrayList<Tool>(ADD_TOOLS.length + DESCRIPTIONS.length);
			for (Tool a : ADD_TOOLS) {
				ret.add(a);
			}
			ret.addAll(FactoryDescription.getTools(Wiring.class, DESCRIPTIONS));
			tools = ret;
		}
		return tools;
	}
}
