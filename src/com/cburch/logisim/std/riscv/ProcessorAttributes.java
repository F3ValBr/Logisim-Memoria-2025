package com.cburch.logisim.std.riscv;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.proj.Project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

class ProcessorAttributes extends AbstractAttributeSet {
	private static final List<Attribute<?>> ATTRIBUTES =
			new ArrayList<>(Arrays.asList(
					Processor.CONTENTS_ATTR,
					Processor.ENABLE_MEMORY,
					Processor.FIRST_ADDR,
					Processor.BOOT_ADDR,
					Processor.INTR_ADDR,
					Processor.ADDR_ATTR,
					Processor.MULTIPLIER));
	
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
	private AttributeOption displayRegister =RV32IM.HIDE_REGISTER;
	private AttributeOption enableMemory=Processor.ENABLE;
	private Integer firstAddr=0;
	private Integer bootAddr=0;
	private Integer intrAddr=0;
	private BitWidth addrBits=BitWidth.create(10);
	private DataContents contents;
	private Integer multiplier=1;
	ProcessorAttributes() {
		contents = DataContents.create(addrBits.getWidth(),32);
	}
	ProcessorAttributes(List<Attribute<?>> attributes) {
		contents = DataContents.create(addrBits.getWidth(),32);
		for(Attribute<?> attr: attributes){
			if(!ATTRIBUTES.contains(attr)){
				ATTRIBUTES.add(attr);
			}
		}
	}
	void setProject(Project proj) {
		register(contents, proj);
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		ProcessorAttributes d = (ProcessorAttributes) dest;
		d.enableMemory=enableMemory;
		d.firstAddr=firstAddr;
		d.bootAddr=bootAddr;
		d.intrAddr=intrAddr;
		d.displayRegister = displayRegister;
		d.addrBits = addrBits;
		d.contents = contents.clone();
		d.multiplier=multiplier;
	}
	
	@Override
	public List<Attribute<?>> getAttributes() {
		return ATTRIBUTES;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		if (attr == RV32IM.DISPLAY_REGISTER) return (V) displayRegister;
		else if (attr == Processor.ENABLE_MEMORY) return (V) enableMemory;
		else if (attr == Processor.FIRST_ADDR) return (V) firstAddr;
		else if (attr == Processor.BOOT_ADDR) return (V) bootAddr;
		else if (attr == Processor.INTR_ADDR) return (V) intrAddr;
		else if (attr ==Processor.ADDR_ATTR) return (V) addrBits;
		else if (attr == Processor.CONTENTS_ATTR) return (V) contents;
		else if (attr == Processor.MULTIPLIER) return (V) multiplier;
		return null;
	}
	
	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == RV32IM.DISPLAY_REGISTER) displayRegister = (AttributeOption) value;
		else if (attr == Processor.ENABLE_MEMORY) enableMemory = (AttributeOption) value;
		else if (attr ==Processor.FIRST_ADDR) firstAddr = (Integer) value;
		else if (attr == Processor.BOOT_ADDR) bootAddr = (Integer) value;
		else if (attr == Processor.INTR_ADDR) intrAddr = (Integer) value;
		else if (attr == Processor.MULTIPLIER) multiplier = (Integer) value;
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
