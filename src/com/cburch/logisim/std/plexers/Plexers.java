/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.plexers;

import java.awt.Graphics;
import java.util.List;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.GraphicsUtil;

public class Plexers extends Library {

    public static final String LIB_NAME = "Plexers";

    public static final Attribute<BitWidth> ATTR_SELECT
		= Attributes.forBitWidth("select", Strings.getter("plexerSelectBitsAttr"), 1, 5);
	public static final Object DEFAULT_SELECT = BitWidth.create(1);

	public static final Attribute<Boolean> ATTR_TRISTATE
		= Attributes.forBoolean("tristate", Strings.getter("plexerThreeStateAttr"));
	public static final Object DEFAULT_TRISTATE = Boolean.FALSE;
	
	public static final AttributeOption DISABLED_FLOATING
		= new AttributeOption("Z", Strings.getter("plexerDisabledFloating"));
	public static final AttributeOption DISABLED_ZERO
		= new AttributeOption("0", Strings.getter("plexerDisabledZero"));
	public static final Attribute<AttributeOption> ATTR_DISABLED
		= Attributes.forOption("disabled", Strings.getter("plexerDisabledAttr"),
				new AttributeOption[] { DISABLED_FLOATING, DISABLED_ZERO });
	
	public static final Attribute<Boolean> ATTR_ENABLE
		= Attributes.forBoolean("enable", Strings.getter("plexerEnableAttr"));

	static final AttributeOption SELECT_BOTTOM_LEFT
		= new AttributeOption("bl", Strings.getter("plexerSelectBottomLeftOption"));
	static final AttributeOption SELECT_TOP_RIGHT
		= new AttributeOption("tr", Strings.getter("plexerSelectTopRightOption"));
	static final Attribute<AttributeOption> ATTR_SELECT_LOC = Attributes.forOption("selloc",
			Strings.getter("plexerSelectLocAttr"),
			new AttributeOption[] { SELECT_BOTTOM_LEFT, SELECT_TOP_RIGHT });

	protected static final int DELAY = 3;

    // ==== IDs públicos de Plexers ====
    public static final String MULTIPLEXER_ID      = Multiplexer._ID;
    public static final String DEMULTIPLEXER_ID    = Demultiplexer._ID;
    public static final String DECODER_ID          = Decoder._ID;
    public static final String PRIORITYENC_ID      = PriorityEncoder._ID;
    public static final String BITSELECTOR_ID      = BitSelector._ID;

    // ==== Descriptions ====
    private static final FactoryDescription[] DESCRIPTIONS = {
        new FactoryDescription(MULTIPLEXER_ID,
            Strings.getter("multiplexerComponent"),
            "multiplexer.gif",
            Multiplexer.class.getSimpleName()
        ),
        new FactoryDescription(DEMULTIPLEXER_ID,
            Strings.getter("demultiplexerComponent"),
            "demultiplexer.gif",
            Demultiplexer.class.getSimpleName()
        ),
        new FactoryDescription(DECODER_ID,
            Strings.getter("decoderComponent"),
            "decoder.gif",
            Decoder.class.getSimpleName()
        ),
        new FactoryDescription(PRIORITYENC_ID,
            Strings.getter("priorityEncoderComponent"),
            "priencod.gif",
            PriorityEncoder.class.getSimpleName()
        ),
        new FactoryDescription(BITSELECTOR_ID,
            Strings.getter("bitSelectorComponent"),
            "bitSelector.gif",
            BitSelector.class.getSimpleName()
        ),
    };

    private List<Tool> tools = null;

	public Plexers() { }

	@Override
	public String getName() { return LIB_NAME; }

	@Override
	public String getDisplayName() { return Strings.get("plexerLibrary"); }

	@Override
	public List<Tool> getTools() {
		if (tools == null) {
			tools = FactoryDescription.getTools(Plexers.class, DESCRIPTIONS);
		}
		return tools;
	}

	static void drawTrapezoid(Graphics g, Bounds bds, Direction facing,
			int facingLean) {
		int wid = bds.getWidth();
		int ht = bds.getHeight();
		int x0 = bds.getX(); int x1 = x0 + wid;
		int y0 = bds.getY(); int y1 = y0 + ht;
		int[] xp = { x0, x1, x1, x0 };
		int[] yp = { y0, y0, y1, y1 };
		if (facing == Direction.WEST) {
			yp[0] += facingLean; yp[3] -= facingLean;
		} else if (facing == Direction.NORTH) {
			xp[0] += facingLean; xp[1] -= facingLean;
		} else if (facing == Direction.SOUTH) {
			xp[2] -= facingLean; xp[3] += facingLean;
		} else {
			yp[1] += facingLean; yp[2] -= facingLean;
		}
		GraphicsUtil.switchToWidth(g, 2);
		g.drawPolygon(xp, yp, 4);
	}
	
	static boolean contains(Location loc, Bounds bds, Direction facing) {
		if (bds.contains(loc, 1)) {
			int x = loc.getX();
			int y = loc.getY();
			int x0 = bds.getX();
			int x1 = x0 + bds.getWidth();
			int y0 = bds.getY();
			int y1 = y0 + bds.getHeight();
			if (facing == Direction.NORTH || facing == Direction.SOUTH) {
				if (x < x0 + 5 || x > x1 - 5) {
					if (facing == Direction.SOUTH) {
						return y < y0 + 5;
					} else {
						return y > y1 - 5;
					}
				} else {
					return true;
				}
			} else {
				if (y < y0 + 5 || y > y1 - 5) {
					if (facing == Direction.EAST) {
						return x < x0 + 5;
					} else {
						return x > x1 - 5;
					}
				} else {
					return true;
				}
			}
		} else {
			return false;
		}
	}
}
