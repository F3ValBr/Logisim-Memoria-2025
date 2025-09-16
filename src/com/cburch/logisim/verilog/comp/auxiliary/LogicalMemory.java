package com.cburch.logisim.verilog.comp.auxiliary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LogicalMemory {
    private final String memId;

    // Índices dentro de mod.cells()
    private int arrayCellIdx = -1;             // $mem / $mem_v2 si existe
    private final List<Integer> readPortIdxs  = new ArrayList<>(); // $memrd{,_v2}
    private final List<Integer> writePortIdxs = new ArrayList<>(); // $memwr{,_v2}
    private final List<Integer> initIdxs      = new ArrayList<>(); // $meminit{,_v2}

    private MemoryMeta meta;

    // --- Mutadores públicos (para usar desde MemoryIndex) ---
    public void markArrayCell(int idx)      { this.arrayCellIdx = idx; }
    public void addReadPort(int idx)        { this.readPortIdxs.add(idx); }
    public void addWritePort(int idx)       { this.writePortIdxs.add(idx); }
    public void addInitCell(int idx)        { this.initIdxs.add(idx); }
    public void setMeta(MemoryMeta meta)    { this.meta = meta; }

    public LogicalMemory(String memId) { this.memId = memId; }

    public String memId() { return memId; }
    public int arrayCellIdx() { return arrayCellIdx; }
    public List<Integer> readPortIdxs()  { return Collections.unmodifiableList(readPortIdxs); }
    public List<Integer> writePortIdxs() { return Collections.unmodifiableList(writePortIdxs); }
    public List<Integer> initIdxs()      { return Collections.unmodifiableList(initIdxs); }
    public MemoryMeta meta() { return meta; }

    @Override public String toString() {
        return "LogicalMemory{" + memId + ", arr=" + arrayCellIdx
                + ", rd=" + readPortIdxs + ", wr=" + writePortIdxs
                + ", init=" + initIdxs + ", meta=" + meta + "}";
    }
}
