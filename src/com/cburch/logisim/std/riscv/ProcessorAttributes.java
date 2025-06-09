package com.cburch.logisim.std.riscv;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.proj.Project;

import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

class ProcessorAttributes extends AbstractAttributeSet {
	private static final List<Attribute<?>> ATTRIBUTES =
			Arrays.asList(Processor.CONTENTS_ATTR,Processor.DISPLAY_REGISTER,Processor.BOOT_ADDR,Processor.ADDR_ATTR);
	
	private static final WeakHashMap<DataContents, ProcessorContentsListener> listenerRegistry
		= new WeakHashMap<DataContents, ProcessorContentsListener>();
	private static final WeakHashMap<DataContents,HexFrame> windowRegistry
		= new WeakHashMap<DataContents,HexFrame>();

	static void register(DataContents value, Project proj) {
		if (proj == null || listenerRegistry.containsKey(value)) return;
		ProcessorContentsListener l = new ProcessorContentsListener(proj);
		value.addHexModelListener(l);
		listenerRegistry.put(value, l);
	}
	
	static HexFrame getHexFrame(DataContents value, Project proj) {
		synchronized(windowRegistry) {
			HexFrame ret = windowRegistry.get(value);
			if (ret == null) {
				ret = new HexFrame(proj, value);
				windowRegistry.put(value, ret);
			}
			return ret;
		}
	}
	private AttributeOption displayRegister =Processor.HIDE_REGISTER;
	private Integer bootAddr=0;
	private BitWidth addrBits=BitWidth.create(10);
	private DataContents contents;

	ProcessorAttributes() {
		contents = DataContents.create(addrBits.getWidth(),32);
	}
	
	void setProject(Project proj) {
		register(contents, proj);
	}
	
	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		ProcessorAttributes d = (ProcessorAttributes) dest;
		d.bootAddr=bootAddr;
		d.displayRegister = displayRegister;
		d.addrBits = addrBits;
		d.contents = contents.clone();
	}
	
	@Override
	public List<Attribute<?>> getAttributes() {
		return ATTRIBUTES;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		if (attr == Processor.DISPLAY_REGISTER) return (V) displayRegister;
		else if (attr == Processor.BOOT_ADDR) return (V) bootAddr;
		else if (attr ==Processor.ADDR_ATTR) return (V) addrBits;
		else if (attr == Processor.CONTENTS_ATTR) return (V) contents;
		return null;
	}
	
	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == Processor.DISPLAY_REGISTER) {
			displayRegister = (AttributeOption) value;
		}
		else if (attr == Processor.BOOT_ADDR) bootAddr = (Integer) value;
		else if (attr == Processor.ADDR_ATTR) {
			addrBits = (BitWidth) value;
			contents.setDimensions(addrBits.getWidth(), 32);
		}
		else if (attr == Processor.CONTENTS_ATTR) {
			contents = (DataContents) value;
		}
		fireAttributeValueChanged(attr, value);
	}
}
