package com.cburch.logisim.verilog.layout.auxiliary;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.verilog.comp.auxiliary.ModulePort;
import com.cburch.logisim.verilog.comp.auxiliary.SupportsFactoryLookup;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.std.ComponentAdapter;
import com.cburch.logisim.verilog.std.ComponentAdapterRegistry;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultNodeSizer implements NodeSizer {
    private final ComponentAdapterRegistry adapters;
    // cache por tipo (para no recalcular cada vez)
    private final Map<String, Dimension> byTypeCache = new HashMap<>();

    public DefaultNodeSizer(ComponentAdapterRegistry adapters) {
        this.adapters = Objects.requireNonNull(adapters);
    }

    @Override
    public Dimension sizeForCell(Project proj, VerilogCell cell) {
        // Cache por typeId (suficientemente buena para la mayoría)
        String key = cell.type().typeId();
        var cached = byTypeCache.get(key);
        if (cached != null) return cached;

        // 1) intentar obtener un factory desde el adapter responsable
        ComponentFactory f = peekFactoryForCell(proj, cell);
        if (f == null) {
            // fallback razonable
            Dimension d = new Dimension(60, 40);
            byTypeCache.put(key, d);
            return d;
        }

        // 2) preparar atributos (ancho si aplica)
        AttributeSet attrs = f.createAttributeSet();
        try {
            int w = Math.max(1, guessWidth(cell.params()));
            attrs.setValue(StdAttr.WIDTH, BitWidth.create(w));
        } catch (Exception ignore) {}

        // 3) consultar tamaño “real” via offset bounds
        Bounds b = probeBounds(f, attrs);
        Dimension d = new Dimension(b.getWidth(), b.getHeight());
        byTypeCache.put(key, d);
        return d;
    }

    @Override
    public Dimension sizeForTopPort(ModulePort port) {
        // Sencillo: un “pin” compacto
        return new Dimension(Math.max(20, 8 + port.width()*2), 20);
    }

    /* ===== helpers ===== */

    private static int guessWidth(CellParams params) {
        if (params instanceof GenericCellParams g) {
            Object yw = g.asMap().get("Y_WIDTH");
            Object aw = g.asMap().get("A_WIDTH");
            int y = parseIntRelaxed(yw, 1);
            int a = parseIntRelaxed(aw, y > 0 ? y : 1);
            return Math.max(1, Math.max(a, y));
        }
        return 1;
    }

    private ComponentFactory peekFactoryForCell(Project proj, VerilogCell cell) {
        // Le pedimos al registry que nos diga qué adapter lo aceptaría
        for (ComponentAdapter a : adapters.getAdapters()) {
            if (a.accepts(cell.type()) && a instanceof SupportsFactoryLookup sfl) {
                return sfl.peekFactory(proj, cell); // puede devolver null
            }
        }
        return null;
    }

    private static Bounds probeBounds(ComponentFactory factory, AttributeSet attrs) {
        try {
            return factory.getOffsetBounds(attrs);
        } catch (Exception e) {
            return Bounds.create(0, 0, 60, 40); // fallback robusto
        }
    }

    // reutiliza tu helper existente
    private static int parseIntRelaxed(Object o, int def) {
        if (o == null) return def;
        try {
            if (o instanceof Number n) return n.intValue();
            String s = String.valueOf(o).trim();
            if (s.startsWith("0b") || s.startsWith("0B")) return Integer.parseInt(s.substring(2), 2);
            if (s.startsWith("0x") || s.startsWith("0X")) return Integer.parseInt(s.substring(2), 16);
            return Integer.parseInt(s);
        } catch (Exception ignore) { return def; }
    }
}
