/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.arith;

import java.util.List;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Arithmetic extends Library {

    public static final String LIB_NAME = "Arithmetic";

    // IDs públicos para cada componente de la librería Arithmetic.
    public static final String ADDER_ID       = Adder._ID;
    public static final String SUBTRACTOR_ID  = Subtractor._ID;
    public static final String MULTIPLIER_ID  = Multiplier._ID;
    public static final String DIVIDER_ID     = Divider._ID;
    public static final String NEGATOR_ID     = Negator._ID;
    public static final String COMPARATOR_ID  = Comparator._ID;
    public static final String SHIFTER_ID     = Shifter._ID;
    public static final String BIT_ADDER_ID   = BitAdder._ID;
    public static final String BIT_FINDER_ID  = BitFinder._ID;

    private static final FactoryDescription[] DESCRIPTIONS = {
        new FactoryDescription(ADDER_ID,
            Strings.getter("adderComponent"),
            "adder.gif",
            Adder.class.getSimpleName()),
        new FactoryDescription(SUBTRACTOR_ID,
            Strings.getter("subtractorComponent"),
            "subtractor.gif",
            Subtractor.class.getSimpleName()),
        new FactoryDescription(MULTIPLIER_ID,
            Strings.getter("multiplierComponent"),
            "multiplier.gif",
            Multiplier.class.getSimpleName()),
        new FactoryDescription(DIVIDER_ID,
            Strings.getter("dividerComponent"),
            "divider.gif",
            Divider.class.getSimpleName()),
        new FactoryDescription(NEGATOR_ID,
            Strings.getter("negatorComponent"),
            "negator.gif",
            Negator.class.getSimpleName()),
        new FactoryDescription(COMPARATOR_ID,
            Strings.getter("comparatorComponent"),
            "comparator.gif",
            Comparator.class.getSimpleName()),
        new FactoryDescription(SHIFTER_ID,
            Strings.getter("shifterComponent"),
            "shifter.gif",
            Shifter.class.getSimpleName()),
        new FactoryDescription(BIT_ADDER_ID,
            Strings.getter("bitAdderComponent"),
            "bitadder.gif",
            BitAdder.class.getSimpleName()),
        new FactoryDescription(BIT_FINDER_ID,
            Strings.getter("bitFinderComponent"),
            "bitfindr.gif",
            BitFinder.class.getSimpleName()),
    };

    private List<Tool> tools = null;

	public Arithmetic() { }

	@Override
	public String getName() { return LIB_NAME; }

	@Override
	public String getDisplayName() { return Strings.get("arithmeticLibrary"); }

	@Override
	public List<Tool> getTools() {
		if (tools == null) {
			tools = FactoryDescription.getTools(Arithmetic.class, DESCRIPTIONS);
		}
		return tools;
	}
}
