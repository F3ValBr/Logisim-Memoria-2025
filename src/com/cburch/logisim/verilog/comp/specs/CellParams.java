package com.cburch.logisim.verilog.comp.specs;

import java.util.Map;

public interface CellParams {
    Object get(String key);
    boolean has(String key);
    Map<String, Object> asMap();

    int getInt(String key, int def);
    long getLong(String key, long def);
    boolean getBool(String key, boolean def);
    String getString(String key, String def);

    void set(String key, Object value);
    void setAll(Map<String, ?> values);
}
