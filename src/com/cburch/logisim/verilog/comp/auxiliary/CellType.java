package com.cburch.logisim.verilog.comp.auxiliary;

import com.cburch.logisim.verilog.comp.specs.gatelvl.GateOp;
import com.cburch.logisim.verilog.comp.specs.gatelvl.RegisterGateOp;
import com.cburch.logisim.verilog.comp.specs.wordlvl.*;

import java.util.*;

/**
 * CellType — Classification of a Yosys cell by abstraction level and semantic kind.
 *
 * <h2>Abstraction Levels</h2>
 * Cells extracted from a Yosys netlist are grouped into one of the following
 * abstraction layers:
 *
 * <ul>
 *   <li><b>Word-level</b> — Operations manipulating multi-bit vectors:
 *       arithmetic ($add, $sub), bitwise ($and, $or), shifts ($shl, $shr),
 *       multiplexers ($mux), memories ($mem), etc.</li>
 *
 *   <li><b>Gate-level</b> — Primitive digital gates and sequential elements:
 *       logic gates ($_AND_, $_OR_), AOI/OAI variants, and flip-flops
 *       ($_DFF_, $_DFFE_, ...).</li>
 *
 *   <li><b>Module instances</b> — Any user-defined Verilog module or unclassified
 *       primitive that is not recognized as a word- or gate-level built-in.</li>
 * </ul>
 *
 * <h2>Semantic Kinds</h2>
 * Within each abstraction level, cells are further categorized by their logical role:
 *
 * <ul>
 *   <li><b>Unary</b> — One-input word-level operators such as $not or $neg.</li>
 *
 *   <li><b>Binary</b> — Two-input word-level operators such as $add, $and, $xor.</li>
 *
 *   <li><b>Simple Gate</b> — Basic gate-level primitives such as $_AND_, $_OR_. </li>
 *
 *   <li><b>Complex Gate</b> — Compound logic structures like $_AOI21_, $_OAI22_. </li>
 *
 *   <li><b>Flip-Flop</b> — Gate-level storage elements ($_DFF_, $_DFFE_, etc.).</li>
 *
 *   <li><b>Register</b> — Word-level sequencing elements like $dff, $dffe.</li>
 *
 *   <li><b>Multiplexer</b> — Both gate-level (e.g. $_MUX4_) and word-level ($mux) multiplexers.</li>
 *
 *   <li><b>Memory</b> — Word-level memory blocks such as $mem, $memrd, $memwr.</li>
 *
 *   <li><b>Other</b> — Any construct not fitting the categories above.</li>
 * </ul>
 *
 * <h2>Purpose</h2>
 * The method {@code fromYosys(typeId)} performs the actual classification,
 * interpreting the Yosys cell type string and mapping it onto the categories above.
 * This classification is fundamental for adapter layers (e.g., UnaryOpAdapter,
 * BinaryOpAdapter, GateAdapter) that select appropriate Logisim component
 * factories and port mappings for each Yosys cell.
 */
public final class CellType {

    public enum Level { WORD, GATE, MODULE }
    public enum Kind  { UNARY, BINARY, SIMPLE_GATE, COMPLEX_GATE, FLIP_FLOP, REGISTER, MULTIPLEXER, MEMORY, OTHER }

    private final String typeId;    // Yosys ID for cell
    private final Level level;      // Abstraction level
    private final Kind kind;        // Classification within level

    private CellType(String typeId, Level level, Kind kind) {
        this.typeId = Objects.requireNonNull(typeId);
        this.level  = Objects.requireNonNull(level);
        this.kind   = Objects.requireNonNull(kind);
    }

    public String typeId() {
        return typeId;
    }
    public Level level()  {
        return level;
    }
    public Kind kind()   {
        return kind;
    }

    public boolean isWordLevel()   { return level == Level.WORD; }
    public boolean isGateLevel()   { return level == Level.GATE; }
    public boolean isModuleInst()  { return level == Level.MODULE; }
    public boolean isUnary()       { return kind  == Kind.UNARY; }
    public boolean isBinary()      { return kind  == Kind.BINARY; }
    public boolean isRegister()    { return kind  == Kind.REGISTER; }
    public boolean isMultiplexer() { return kind  == Kind.MULTIPLEXER; }
    public boolean isMemory()      { return kind  == Kind.MEMORY; }
    public boolean isSimpleGate()  { return kind  == Kind.SIMPLE_GATE; }
    public boolean isComplexGate() { return kind  == Kind.COMPLEX_GATE; }
    public boolean isFlipFlop()    { return kind  == Kind.FLIP_FLOP; }

    /** Main classifier from Yosys type ID
     * Determines the CellType (level and kind) from a Yosys cell type ID.
     *
     * @param typeId Yosys cell type ID
     * @return CellType instance
     */
    public static CellType fromYosys(String typeId) {
        if (typeId == null || typeId.isEmpty()) {
            return new CellType("<unknown>", Level.MODULE, Kind.OTHER);
        }

        // Gate-level ($_)
        if (typeId.startsWith("$_")) {
            // Gate kind
            if (GateOp.isGateTypeId(typeId)) {
                GateOp op = GateOp.fromYosys(typeId);
                return switch (op.category()) {
                    case SIMPLE      -> new CellType(typeId, Level.GATE, Kind.SIMPLE_GATE);
                    case COMBINED    -> new CellType(typeId, Level.GATE, Kind.COMPLEX_GATE);
                    case MUX_FAMILY  -> new CellType(typeId, Level.GATE, Kind.MULTIPLEXER);
                };
            }
            if (RegisterGateOp.matchesRGOp(typeId)) {
                return new CellType(typeId, Level.GATE, Kind.FLIP_FLOP);
            }
            // Unknown Gate-level
            return new CellType(typeId, Level.GATE, Kind.OTHER);
        }

        // Word-level ($)
        if (typeId.startsWith("$")) {
            if (UnaryOp.isUnaryTypeId(typeId)) {
                return new CellType(typeId, Level.WORD, Kind.UNARY);
            }
            if (BinaryOp.isBinaryTypeId(typeId)) {
                return new CellType(typeId, Level.WORD, Kind.BINARY);
            }
            if (MuxOp.isMuxTypeId(typeId)) {
                return new CellType(typeId, Level.WORD, Kind.MULTIPLEXER);
            }
            if (RegisterOp.isRegisterTypeId(typeId)) {
                return new CellType(typeId, Level.WORD, Kind.REGISTER);
            }
            if (MemoryOp.isMemoryTypeId(typeId)) {
                return new CellType(typeId, Level.WORD, Kind.MEMORY);
            }
            // Unknown Word-level
            return new CellType(typeId, Level.WORD, Kind.OTHER);
        }

        // Module instance (anything else)
        return new CellType(typeId, Level.MODULE, Kind.OTHER);
    }

    @Override
    public String toString() {
        return "CellType{" + typeId + ", level=" + level + ", kind=" + kind + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CellType that)) return false;
        return typeId.equals(that.typeId) && level == that.level && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeId, level, kind);
    }
}
