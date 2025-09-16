package com.cburch.logisim.verilog.std.adapters.wordlvl;

// RegisterOpAdapter.java

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.*;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.FactoryLookup;
import com.cburch.logisim.verilog.comp.auxiliary.SupportsFactoryLookup;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.std.*;
import com.cburch.logisim.verilog.std.adapters.ModuleBlackBoxAdapter;

import java.awt.Graphics;
import java.util.Locale;
import java.util.Map;

public final class RegisterOpAdapter extends AbstractComponentAdapter
                                     implements SupportsFactoryLookup {

    // Names esperados en tu Register (cámbialos si usaste otros):
    private static final String A_REG_HAS_EN       = "regHasEnable";
    private static final String A_EN_POLARITY      = "enablePolarity";
    private static final String A_RESET_TYPE       = "resetType";
    private static final String A_RESET_POLARITY   = "resetPolarity";
    private static final String A_RESET_VALUE      = "resetValue";

    private static final String OPT_EN_ACTIVE_HIGH = "enActiveHigh";
    private static final String OPT_EN_ACTIVE_LOW  = "enActiveLow";

    private static final String OPT_RST_ASYNC      = "asyncReset";
    private static final String OPT_RST_SYNC       = "syncReset";
    private static final String OPT_RST_NONE       = "noReset";

    private static final String OPT_RST_ACTIVE_HIGH= "rstActiveHigh";
    private static final String OPT_RST_ACTIVE_LOW = "rstActiveLow";

    private final ModuleBlackBoxAdapter fallback = new ModuleBlackBoxAdapter();

    @Override
    public boolean accepts(CellType t) {
        return t != null && t.isWordLevel() && t.isRegister();
    }

    @Override
    public InstanceHandle create(Canvas canvas, Graphics g, VerilogCell cell, Location where) {
        ComponentFactory factory = pickRegisterFactory(canvas.getProject());
        if (factory == null) {
            return fallback.create(canvas, g, cell, where);
        }

        // ====== Params/puertos Yosys ======
        final CellParams params = cell.params();
        final String typeId = cell.type().typeId().toLowerCase(Locale.ROOT);

        final int width = Math.max(1, guessWidth(params));
        final boolean hasEnPort = hasPort(cell, "EN")
                || typeId.contains("dffe") || typeId.contains("sdffe")
                || typeId.contains("aldffe") || typeId.contains("adffe")
                || typeId.contains("dffsre") || typeId.contains("sdffce");

        final Boolean enActiveHigh = readBitDefault(params, "EN_POLARITY", true);
        final boolean clkRising    = readBitDefault(params, "CLK_POLARITY", true);

        final ResetInfo rst = detectReset(cell);

        try {
            Project proj = canvas.getProject();
            Circuit circ = canvas.getCircuit();
            AttributeSet attrs = factory.createAttributeSet();

            // Básicos públicos
            safeSet(attrs, StdAttr.WIDTH, BitWidth.create(width));
            safeSet(attrs, StdAttr.LABEL, cleanCellName(cell.name()));
            safeSet(attrs, StdAttr.TRIGGER, clkRising ? StdAttr.TRIG_RISING : StdAttr.TRIG_FALLING);

            // Enable visible + polaridad (por name)
            setBooleanByName(attrs, A_REG_HAS_EN, hasEnPort);
            if (hasEnPort) {
                setOptionByName(attrs, A_EN_POLARITY,
                        (enActiveHigh != null && enActiveHigh) ? OPT_EN_ACTIVE_HIGH : OPT_EN_ACTIVE_LOW);
            }

            // Reset (por name)
            switch (rst.kind) {
                case NONE -> setOptionByName(attrs, A_RESET_TYPE, OPT_RST_NONE);
                case ASYNC -> {
                    setOptionByName(attrs, A_RESET_TYPE, OPT_RST_ASYNC);
                    setOptionByName(attrs, A_RESET_POLARITY, rst.activeHigh ? OPT_RST_ACTIVE_HIGH : OPT_RST_ACTIVE_LOW);
                    setStringByName(attrs, A_RESET_VALUE, rst.valueText);
                }
                case SYNC -> {
                    setOptionByName(attrs, A_RESET_TYPE, OPT_RST_SYNC);
                    setOptionByName(attrs, A_RESET_POLARITY, rst.activeHigh ? OPT_RST_ACTIVE_HIGH : OPT_RST_ACTIVE_LOW);
                    setStringByName(attrs, A_RESET_VALUE, rst.valueText);
                }
            }

            Component comp = addComponent(proj, circ, g, factory, where, attrs);
            PinLocator pins = (port, bit) -> comp.getLocation(); // placeholder
            return new InstanceHandle(comp, pins);

        } catch (CircuitException e) {
            throw new IllegalStateException("No se pudo añadir Register: " + e.getMessage(), e);
        }
    }

    @Override
    public ComponentFactory peekFactory(Project proj, VerilogCell cell) {
        return pickRegisterFactory(proj);
    }

    /* ================= helpers ================= */

    private static ComponentFactory pickRegisterFactory(Project proj) {
        LogisimFile lf = proj.getLogisimFile();
        if (lf == null) return null;
        Library mem = lf.getLibrary("Memory");
        if (mem == null) return null;

        return FactoryLookup.findFactory(mem, "Register");
    }

    private static int guessWidth(CellParams p) {
        if (p instanceof GenericCellParams g) {
            Object w = g.asMap().get("WIDTH");
            return parseIntRelaxed(w, 1);
        }
        return 1;
    }

    private static boolean hasPort(VerilogCell cell, String name) {
        for (String p : cell.getPortNames()) {
            if (p.equals(name)) return true;
        }
        return false;
    }

    /** Detecta tipo/polaridad/valor de reset a partir de typeId y parámetros Yosys. */
    private static ResetInfo detectReset(VerilogCell cell) {
        Map<String, Object> m = (cell.params() instanceof GenericCellParams g) ? g.asMap() : Map.of();
        String t = cell.type().typeId().toLowerCase(Locale.ROOT);

        // Heurística por presencia de parámetros
        boolean hasArst = m.containsKey("ARST_VALUE") || m.containsKey("ARST_POLARITY");
        boolean hasSrst = m.containsKey("SRST_VALUE") || m.containsKey("SRST_POLARITY");

        if (t.contains("adff") || t.contains("aldff") || t.contains("adffe") || t.contains("aldffe") || hasArst) {
            boolean pol = readBitDefault(cell.params(), "ARST_POLARITY", true);
            String val  = stringDefault(m.get("ARST_VALUE"), "0");
            return ResetInfo.async(pol, val);
        }
        if (t.contains("sdff") || t.contains("sdffe") || t.contains("sdffce") || hasSrst) {
            boolean pol = readBitDefault(cell.params(), "SRST_POLARITY", true);
            String val  = stringDefault(m.get("SRST_VALUE"), "0");
            return ResetInfo.sync(pol, val);
        }
        // TODO: $dffsr / $dffsre: modela como async por simplicidad (ajusta si necesitas)
        if (t.contains("dffsr") || t.contains("dffsre")) {
            // si hay SRST/ARST_* en params, respétalos
            return ResetInfo.none();
        }
        return ResetInfo.none();
    }

    /* ===== utilidades comunes ===== */

    static <T> void safeSet(AttributeSet attrs, Attribute<T> attr, T val) {
        try { attrs.setValue(attr, val); } catch (Exception ignore) { }
    }

    static boolean readBitDefault(CellParams p, String key, boolean def) {
        if (!(p instanceof GenericCellParams g)) return def;
        Object v = g.asMap().get(key);
        if (v == null) return def;
        int i = parseIntRelaxed(v, def ? 1 : 0);
        return i != 0;
    }

    static String stringDefault(Object v, String def) {
        if (v == null) return def;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? def : s;
    }

    /* ===== modelo simple de reset ===== */

    private static final class ResetInfo {
        enum Kind { NONE, ASYNC, SYNC }
        final Kind kind;
        final boolean activeHigh;
        final String valueText;

        private ResetInfo(Kind kind, boolean activeHigh, String valueText) {
            this.kind = kind; this.activeHigh = activeHigh; this.valueText = valueText;
        }
        static ResetInfo none()                    { return new ResetInfo(Kind.NONE,  true, "0"); }
        static ResetInfo async(boolean hi, String v){ return new ResetInfo(Kind.ASYNC, hi, v); }
        static ResetInfo sync (boolean hi, String v){ return new ResetInfo(Kind.SYNC,  hi, v); }
    }
}
