package com.cburch.logisim.verilog.layout;

import com.cburch.logisim.verilog.comp.auxiliary.LogicalMemory;
import com.cburch.logisim.verilog.comp.auxiliary.MemoryMeta;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysMemoryDTO;

import java.util.*;

public final class MemoryIndex {
    private final Map<String, LogicalMemory> byId = new LinkedHashMap<>();

    public MemoryIndex(List<VerilogCell> cells) { this(cells, Map.of()); }

    public MemoryIndex(List<VerilogCell> cells, Map<String, YosysMemoryDTO> memories) {
        Objects.requireNonNull(cells, "cells");

        // 1) Agrupar por MEMID desde las celdas
        for (int i = 0; i < cells.size(); i++) {
            VerilogCell c = cells.get(i);
            if (c == null || !c.type().isMemory()) continue;

            String memIdRaw = extractMemId(c.params());
            String memId = normalizeMemId(memIdRaw);
            if (memId == null || memId.isEmpty()) continue;

            LogicalMemory lm = byId.computeIfAbsent(memId, LogicalMemory::new);
            String t = c.type().typeId();

            if ("$mem".equals(t) || "$mem_v2".equals(t)) {
                lm.markArrayCell(i);
            } else if ("$memrd".equals(t) || "$memrd_v2".equals(t)) {
                lm.addReadPort(i);
            } else if ("$memwr".equals(t) || "$memwr_v2".equals(t)) {
                lm.addWritePort(i);
            } else if ("$meminit".equals(t) || "$meminit_v2".equals(t)) {
                lm.addInitCell(i);
            }
        }

        // 2) Fusionar metadata desde "memories" (aunque no existan $mem cells)
        if (memories != null && !memories.isEmpty()) {
            for (var e : memories.entrySet()) {
                String keyRaw = e.getKey();
                String memId = normalizeMemId(keyRaw);
                if (memId == null || memId.isEmpty()) continue;

                YosysMemoryDTO dto = e.getValue();
                if (dto == null) continue;

                LogicalMemory lm = byId.computeIfAbsent(memId, LogicalMemory::new);
                // Construye la meta con tolerancia a nulos
                int width  = safeInt(dto.width(),  0);
                int size   = safeInt(dto.size(),   0);
                int offset = safeInt(dto.startOffset(), 0);
                Map<String,Object> attrs = dto.attributes() == null ? Map.of() : dto.attributes();
                lm.setMeta(new MemoryMeta(width, size, offset, attrs));
            }
        }
    }

    public Collection<LogicalMemory> memories() { return byId.values(); }
    public LogicalMemory get(String memId) { return byId.get(normalizeMemId(memId)); }

    /* ---------------- helpers ---------------- */

    /** Extrae MEMID desde distintos tipos de CellParams. */
    private static String extractMemId(CellParams cp) {
        if (cp == null) return null;

        // Si tienes clases específicas:
        // if (cp instanceof MemParams p) return p.memId();
        // if (cp instanceof MemV2Params p) return p.memId();
        // if (cp instanceof MemRDParams p) return p.memId();
        // if (cp instanceof MemRDV2Params p) return p.memId();
        // if (cp instanceof MemWRParams p) return p.memId();
        // if (cp instanceof MemWRV2Params p) return p.memId();
        // if (cp instanceof MemInitParams p) return p.memId();
        // if (cp instanceof MemInitV2Params p) return p.memId();

        // Fallback genérico (tu GenericCellParams guarda el mapa crudo):
        if (cp instanceof GenericCellParams g) {
            Object v = g.asMap().get("MEMID");
            return v == null ? null : String.valueOf(v);
        }
        return null;
    }

    /** Normaliza nombres de memoria del dump de Yosys: quita backslash inicial y trim. */
    private static String normalizeMemId(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return "";
        // Yosys suele emitir IDs con escape RTLIL: "\mem"
        if (t.length() >= 2 && t.charAt(0) == '\\') {
            t = t.substring(1);
        }
        return t;
    }

    private static int safeInt(Integer v, int def) { return v == null ? def : v; }
}


