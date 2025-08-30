package com.cburch.logisim.verilog.comp.auxiliary;

import com.cburch.logisim.verilog.layout.ModuleNetIndex;

/**
 * Utility class for traversing all endpoints of a net.
 */
public final class NetTraversal {
    public static void visitNet(ModuleNetIndex idx, int netId, EndpointVisitor v) {
        for (int ref : idx.endpointsOf(netId)) {
            if (ModuleNetIndex.isTop(ref)) v.topPort(ModuleNetIndex.ownerIdx(ref), ModuleNetIndex.bitIdx(ref));
            else                           v.cellBit(ModuleNetIndex.ownerIdx(ref), ModuleNetIndex.bitIdx(ref));
        }
    }
}
