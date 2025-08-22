package com.cburch.logisim.verilog.comp.specs;

import java.util.*;

public class GenericCellParams implements CellParams {
    protected final Map<String, Object> map;

    public GenericCellParams() {
        this.map = new LinkedHashMap<>();
    }
    public GenericCellParams(Map<String, ?> init) {
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

    /** Fábrica: parsea el JSON Yosys (Strings bin/hex) normalizando tipos */
    public static GenericCellParams fromYosys(Map<String, String> yosysParams) {
        return new GenericCellParams(yosysParams);
    }
}
