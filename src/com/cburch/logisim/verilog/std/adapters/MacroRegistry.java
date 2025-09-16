package com.cburch.logisim.verilog.std.adapters;

import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.verilog.comp.impl.VerilogCell;
import com.cburch.logisim.verilog.std.adapters.wordlvl.UnaryOpComposer;
import com.cburch.logisim.verilog.std.macrocomponents.ComposeCtx;
import com.cburch.logisim.verilog.std.macrocomponents.Macro;

import java.util.HashMap;
import java.util.Map;

import static com.cburch.logisim.verilog.std.adapters.wordlvl.UnaryOpAdapter.guessUnaryWidth;

/** Registro global/simple de recetas por typeId de Yosys. */
public final class MacroRegistry {

    public interface Recipe {
        Macro build(ComposeCtx ctx, VerilogCell cell, Location where) throws CircuitException;
    }

    private final Map<String, Recipe> map = new HashMap<>();

    public void register(String yosysTypeId, Recipe r){ map.put(yosysTypeId, r); }
    public Recipe find(String yosysTypeId){ return map.get(yosysTypeId); }

    /** Bootstrap con unarias. */
    public static MacroRegistry bootUnaryDefaults() {
        MacroRegistry reg = new MacroRegistry();
        UnaryOpComposer u = new UnaryOpComposer();

        reg.register("$logic_not", (ctx, cell, where) -> {
            int w = guessUnaryWidth(cell.params());
            return u.buildLogicNotEqZero(ctx, cell, where, w);
        });
        reg.register("$reduce_or", (ctx, cell, where) -> {
            int w = guessUnaryWidth(cell.params());
            return u.buildReduceOrNeZero(ctx, cell, where, w);
        });
        reg.register("$reduce_bool", (ctx, cell, where) -> {
            int w = guessUnaryWidth(cell.params());
            return u.buildReduceOrNeZero(ctx, cell, where, w);
        });
        reg.register("$reduce_and", (ctx, cell, where) -> {
            int w = guessUnaryWidth(cell.params());
            return u.buildReduceAndEqAllOnes(ctx, cell, where, w);
        });
        reg.register("$reduce_xor", (ctx, cell, where) -> {
            int w = guessUnaryWidth(cell.params());
            return u.buildReduceXorParity(ctx, cell, where, w, true);
        });
        reg.register("$reduce_xnor", (ctx, cell, where) -> {
            int w = guessUnaryWidth(cell.params());
            return u.buildReduceXorParity(ctx, cell, where, w, false);
        });
        return reg;
    }
}

