package com.cburch.logisim.verilog.comp.specs;

import java.util.*;

/**
 * Generic implementation of CellAttribs using a LinkedHashMap to store key-value pairs.
 * This class provides methods to get, set, and manipulate cell attributes.
 */
public class GenericCellAttribs implements CellAttribs {
    protected final Map<String, Object> map;

    public GenericCellAttribs() {
        this.map = new LinkedHashMap<>();
    }
    public GenericCellAttribs(Map<String, ?> init) {
        this.map = new LinkedHashMap<>();
        if (init != null) setAll(init);
    }

    @Override public Object get(String key) { return map.get(key); }
    @Override public boolean has(String key) { return map.containsKey(key); }
    @Override public Map<String, Object> asMap() { return Collections.unmodifiableMap(map); }

    @Override public int getInt(String key, int def)   { return YosysValues.toInt(map.get(key), def); }
    @Override public long getLong(String key, long def){ return YosysValues.toLong(map.get(key), def); }
    @Override public boolean getBool(String key, boolean def){ return YosysValues.toBool(map.get(key), def); }
    @Override public String getString(String key, String def){ Object v = map.get(key); return v==null?def:String.valueOf(v); }

    @Override public void set(String key, Object value){ map.put(key, value); }
    @Override public void setAll(Map<String, ?> values){ values.forEach((k,v) -> map.put(k, YosysValues.normalize(v))); }

    public static GenericCellAttribs fromYosys(Map<String, String> yosysAttribs) {
        return new GenericCellAttribs(yosysAttribs);
    }
}
