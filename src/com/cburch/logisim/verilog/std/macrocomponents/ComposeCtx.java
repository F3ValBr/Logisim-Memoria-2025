package com.cburch.logisim.verilog.std.macrocomponents;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.proj.Project;

import java.awt.*;

/** Contexto compartido para todos los compositores. */
public final class ComposeCtx {
    public final Project proj;
    public final Circuit circ;
    public final Graphics g;
    public final Factories fx;
    public ComposeCtx(Project p, Circuit c, Graphics g, Factories fx) {
        this.proj = p; this.circ = c; this.g = g; this.fx = fx;
    }
}
