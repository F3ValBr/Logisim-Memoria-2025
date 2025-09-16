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
            Dimension d = new Dimension(60, 60);
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
        final int bits = Math.max(1, port.width());

        // Reglas: 8 bits por fila (→ 4 casillas de 20 px porque 2 bits/casilla), hasta 4 filas.
        final int bitsPerRow = 8;
        final int maxRows = 4;

        // Filas necesarias (cap a 4)
        int rows = Math.min( (bits + bitsPerRow - 1) / bitsPerRow, maxRows );

        // Bits visibles en la fila más ancha (cap a 8)
        int rowBits = Math.min(bits, bitsPerRow);

        // Casillas horizontales: 2 bits por casilla → ceil(rowBits / 2)
        int boxes = (rowBits + 2 - 1) / 2; // ceil

        int w = boxes * 20;   // 1 casilla = 20 px
        int h = rows  * 20;   // 1 fila    = 20 px

        // Límites: mínimo 20×20, máximo 80×80
        w = Math.max(20, Math.min(80, w));
        h = Math.max(20, Math.min(80, h));

        return new Dimension(w, h);
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
        // Ver con registry qué adapter lo aceptaría
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
