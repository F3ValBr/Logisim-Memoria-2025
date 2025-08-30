package com.cburch.logisim.verilog.comp.specs;

public final class YosysValues {
    private YosysValues() {}

    /** Normaliza valores crudos del JSON de Yosys (Strings bin/hex/"0"/"1") a tipos Java */
    public static Object normalize(Object v) {
        if (v == null) return null;
        if (v instanceof Number || v instanceof Boolean) return v;

        String s = v.toString().trim();
        switch (s) {
            case "" -> { return s; }
            case "0" -> { return 0; }
            case "1" -> { return 1; }
        }

        // binarios largos ("000000...00100000") -> int/long si entran
        if (isBinaryString(s)) {
            // si cabe en 31 bits, int; si no, long (o BigInteger si prefieres)
            try { return Integer.parseUnsignedInt(s, 2); }
            catch (NumberFormatException ex) {
                try { return Long.parseUnsignedLong(s, 2); }
                catch (NumberFormatException ex2) { return s; } // deja como string si es enorme
            }
        }

        // hex estilo 0x1A2B...
        if (s.startsWith("0x") || s.startsWith("0X")) {
            try { return Integer.parseUnsignedInt(s.substring(2), 16); }
            catch (NumberFormatException ex) {
                try { return Long.parseUnsignedLong(s.substring(2), 16); }
                catch (NumberFormatException ex2) { return s; }
            }
        }

        return s;
    }

    public static int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        // intenta bin/hex si vino como string
        Object nrm = normalize(v);
        return (nrm instanceof Number n2) ? n2.intValue() : def;
    }

    public static long toLong(Object v, long def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.longValue();
        Object nrm = normalize(v);
        return (nrm instanceof Number n2) ? n2.longValue() : def;
    }

    public static boolean toBool(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.longValue() != 0L;
        String s = v.toString().trim().toLowerCase();
        return switch (s) {
            case "true", "yes", "on" -> true;
            case "false", "no", "off" -> false;
            case "1" -> true;
            case "0" -> false;
            default -> def;
        };
    }

    private static boolean isBinaryString(String s) {
        // acepta cadenas de 0/1 largas
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '0' && c != '1') return false;
        }
        return true;
    }
}

