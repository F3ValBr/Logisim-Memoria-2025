package com.cburch.logisim.verilog.comp.auxiliary;

import java.util.ArrayList;
import java.util.List;

public final class LogicalMemory {
    public final String memId;

    // Índices dentro de mod.cells()
    public int arrayCellIdx = -1;             // $mem / $mem_v2 si existe
    public final List<Integer> readPortIdxs  = new ArrayList<>(); // $memrd{,_v2}
    public final List<Integer> writePortIdxs = new ArrayList<>(); // $memwr{,_v2}
    public final List<Integer> initIdxs      = new ArrayList<>(); // $meminit{,_v2}

    public MemoryMeta meta;

    public LogicalMemory(String memId) { this.memId = memId; }
}
