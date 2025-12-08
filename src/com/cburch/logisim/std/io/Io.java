/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.io;

import java.awt.Color;
import java.util.List;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Io extends Library {

    public static final String LIB_NAME = "I/O";

    static final AttributeOption LABEL_CENTER = new AttributeOption("center", "center", Strings.getter("ioLabelCenter"));
	
	static final Attribute<Color> ATTR_COLOR = Attributes.forColor("color",
			Strings.getter("ioColorAttr"));
	static final Attribute<Color> ATTR_ON_COLOR
		= Attributes.forColor("color", Strings.getter("ioOnColor"));
	static final Attribute<Color> ATTR_OFF_COLOR
		= Attributes.forColor("offcolor", Strings.getter("ioOffColor"));
	static final Attribute<Color> ATTR_BACKGROUND
		= Attributes.forColor("bg", Strings.getter("ioBackgroundColor"));
	static final Attribute<Object> ATTR_LABEL_LOC = Attributes.forOption("labelloc",
			Strings.getter("ioLabelLocAttr"),
			new Object[] { LABEL_CENTER, Direction.NORTH, Direction.SOUTH,
				Direction.EAST, Direction.WEST });
	static final Attribute<Color> ATTR_LABEL_COLOR = Attributes.forColor("labelcolor",
			Strings.getter("ioLabelColorAttr"));
	static final Attribute<Boolean> ATTR_ACTIVE = Attributes.forBoolean("active",
			Strings.getter("ioActiveAttr"));

	static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255, 0);

    // ==== IDs públicos para IO ====
    public static final String BUTTON_ID        = Button._ID;
    public static final String JOYSTICK_ID      = Joystick._ID;
    public static final String KEYBOARD_ID      = Keyboard._ID;
    public static final String LED_ID           = Led._ID;
    public static final String SEVEN_SEGMENT_ID = SevenSegment._ID;
    public static final String HEX_DIGIT_ID     = HexDigit._ID;
    public static final String DOT_MATRIX_ID    = DotMatrix._ID;
    public static final String TTY_ID           = Tty._ID;

    // ==== Descriptions ====
    private static final FactoryDescription[] DESCRIPTIONS = {
        new FactoryDescription(BUTTON_ID,
            Strings.getter("buttonComponent"),
            "button.gif",
            Button.class.getSimpleName()
        ),
        new FactoryDescription(JOYSTICK_ID,
            Strings.getter("joystickComponent"),
            "joystick.gif",
            Joystick.class.getSimpleName()
        ),
        new FactoryDescription(KEYBOARD_ID,
            Strings.getter("keyboardComponent"),
            "keyboard.gif",
            Keyboard.class.getSimpleName()
        ),
        new FactoryDescription(LED_ID,
            Strings.getter("ledComponent"),
            "led.gif",
            Led.class.getSimpleName()
        ),
        new FactoryDescription(SEVEN_SEGMENT_ID,
            Strings.getter("sevenSegmentComponent"),
            "7seg.gif",
            SevenSegment.class.getSimpleName()
        ),
        new FactoryDescription(HEX_DIGIT_ID,
            Strings.getter("hexDigitComponent"),
            "hexdig.gif",
            HexDigit.class.getSimpleName()
        ),
        new FactoryDescription(DOT_MATRIX_ID,
            Strings.getter("dotMatrixComponent"),
            "dotmat.gif",
            DotMatrix.class.getSimpleName()
        ),
        new FactoryDescription(TTY_ID,
            Strings.getter("ttyComponent"),
            "tty.gif",
            Tty.class.getSimpleName()
        ),
    };

    private List<Tool> tools = null;

	public Io() { }

	@Override
	public String getName() { return LIB_NAME; }

	@Override
	public String getDisplayName() { return Strings.get("ioLibrary"); }

	@Override
	public List<Tool> getTools() {
		if (tools == null) {
			tools = FactoryDescription.getTools(Io.class, DESCRIPTIONS);
		}
		return tools;
	}
}
