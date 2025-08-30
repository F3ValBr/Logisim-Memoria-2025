package com.cburch.logisim.verilog.layout;

import com.cburch.logisim.verilog.comp.auxiliary.LogicalMemory;
import com.cburch.logisim.verilog.comp.auxiliary.MemoryMeta;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysMemoryDTO;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MemoryIndex {
    private final Map<String, LogicalMemory> byId = new LinkedHashMap<>();

    // Versión original (solo celdas)
    public MemoryIndex(List<VerilogCell> cells) { this(cells, Map.of()); }

    // Versión enriquecida (celdas + memories del DTO)
    public MemoryIndex(List<VerilogCell> cells, Map<String, YosysMemoryDTO> memories) {
        // 1) Agrupar por MEMID desde las celdas
        for (int i = 0; i < cells.size(); i++) {
            VerilogCell c = cells.get(i);
            if (!c.type().isMemory()) continue;

            String memId = extractMemId(c.params());
            if (memId == null || memId.isBlank()) continue;

            LogicalMemory lm = byId.computeIfAbsent(memId, LogicalMemory::new);
            String t = c.type().typeId();

            if ("$mem".equals(t) || "$mem_v2".equals(t))          lm.arrayCellIdx = i;
            else if ("$memrd".equals(t) || "$memrd_v2".equals(t)) lm.readPortIdxs.add(i);
            else if ("$memwr".equals(t) || "$memwr_v2".equals(t)) lm.writePortIdxs.add(i);
            else if ("$meminit".equals(t) || "$meminit_v2".equals(t)) lm.initIdxs.add(i);
        }

        // 2) Fusionar metadata desde "memories" (aunque no existan $mem cells)
        for (var e : memories.entrySet()) {
            String memId = e.getKey();
            var dto = e.getValue();
            LogicalMemory lm = byId.computeIfAbsent(memId, LogicalMemory::new);
            lm.meta = new MemoryMeta(dto.width(), dto.size(), dto.startOffset(), dto.attributes());
        }
    }

    public Collection<LogicalMemory> memories() { return byId.values(); }
    public LogicalMemory get(String memId) { return byId.get(memId); }

    private static String extractMemId(CellParams cp) {
        if (cp instanceof GenericCellParams g) {
            Object v = g.asMap().get("MEMID");
            return v == null ? null : String.valueOf(v);
        }
        return null;
    }
}

