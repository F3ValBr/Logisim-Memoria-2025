package com.cburch.logisim.verilog.std;

import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;

public class Strings {
    private static LocaleManager source
            = new LocaleManager("resources/logisim", "verilog");

    public static String get(String key) {
        return source.get(key);
    }
    public static StringGetter getter(String key) {
        return source.getter(key);
    }
    public static StringGetter getter(String key, StringGetter arg) {
        return source.getter(key, arg);
    }
}
