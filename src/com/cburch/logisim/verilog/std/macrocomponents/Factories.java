package com.cburch.logisim.verilog.std.macrocomponents;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.verilog.comp.auxiliary.FactoryLookup;

/** Cache de factories (cárgalo una vez por Project). */
public final class Factories {
    public final ComponentFactory cmp;         // Arithmetic → Comparator
    public final ComponentFactory constF;      // Wiring → Constant
    public final ComponentFactory notF;        // Gates → NOT Gate
    public final ComponentFactory oddParityF;  // Gates → Odd Parity
    public final ComponentFactory evenParityF; // Gates → Even Parity
    // puedes añadir más: OR, AND, MUX, Register, etc.

    public static Factories warmup(Project proj) {
        LogisimFile lf = proj.getLogisimFile();
        Library arith = lf.getLibrary("Arithmetic");
        Library wiring= lf.getLibrary("Wiring");
        Library gates = lf.getLibrary("Gates");
        return new Factories(
                arith != null ? FactoryLookup.findFactory(arith, "Comparator") : null,
                wiring!= null ? FactoryLookup.findFactory(wiring, "Constant")  : null,
                gates != null ? FactoryLookup.findFactory(gates, "NOT Gate")   : null,
                gates != null ? FactoryLookup.findFactory(gates, "Odd Parity") : null,
                gates != null ? FactoryLookup.findFactory(gates, "Even Parity"): null
        );
    }
    public Factories(ComponentFactory cmp, ComponentFactory constF, ComponentFactory notF,
                     ComponentFactory oddP, ComponentFactory evenP) {
        this.cmp=cmp; this.constF=constF; this.notF=notF; this.oddParityF=oddP; this.evenParityF=evenP;
    }
}
