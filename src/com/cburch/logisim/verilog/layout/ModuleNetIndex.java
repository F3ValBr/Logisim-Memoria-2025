package com.cburch.logisim.verilog.layout;

import com.cburch.logisim.verilog.comp.VerilogCell;
import com.cburch.logisim.verilog.comp.auxiliary.ModulePort;
import com.cburch.logisim.verilog.comp.auxiliary.PortEndpoint;
import com.cburch.logisim.verilog.comp.auxiliary.netconn.NetBit;

import java.util.*;

public final class ModuleNetIndex {
    // netId -> lista de refs compactos (int)
    private final Map<Integer, List<Integer>> byNet = new LinkedHashMap<>();

    // Codificación compacta de un “pin ref” en 32 bits:
    // bit31..30 = kind (0=cell, 1=top), bit29..16 = ownerIdx (hasta 16383), bit15..8 = reservado (0), bit7..0 = bitIndex (0..255)
    private static int encCell(int cellIdx, int bitIdx) { return (0 << 30) | (cellIdx << 16) | (bitIdx & 0xFF); }
    private static int encTop (int portIdx, int bitIdx) { return (1 << 30) | (portIdx << 16) | (bitIdx & 0xFF); }

    public static boolean isTop(int ref)   { return ((ref >>> 30) & 0b11) == 1; }
    public static int ownerIdx(int ref)    { return (ref >>> 16) & 0x3FFF; }
    public static int bitIdx(int ref)      { return ref & 0xFF; }

    public ModuleNetIndex(List<VerilogCell> cells, List<ModulePort> modulePorts) {
        // 1) Puertos del módulo
        for (int pIdx = 0; pIdx < modulePorts.size(); pIdx++) {
            ModulePort p = modulePorts.get(pIdx);
            for (int i = 0; i < p.width(); i++) {
                int net = p.netIdAt(i);
                if (net >= 0) add(net, encTop(pIdx, i)); // constantes (<0) no se indexan
            }
        }
        // 2) Celdas internas
        for (int cIdx = 0; cIdx < cells.size(); cIdx++) {
            VerilogCell c = cells.get(cIdx);
            for (PortEndpoint ep : c.endpoints()) {
                var br = ep.getBitRef();
                if (br instanceof NetBit nb) {
                    add(nb.getNetId(), encCell(cIdx, ep.getBitIndex()));
                }
            }
        }
    }

    private void add(int netId, int ref) {
        byNet.computeIfAbsent(netId, k -> new ArrayList<>()).add(ref);
    }

    public Set<Integer> netIds() { return byNet.keySet(); }
    public List<Integer> endpointsOf(int netId) {
        return byNet.getOrDefault(netId, List.of());
    }
}

