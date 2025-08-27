package com.cburch.logisim.verilog.comp.auxiliary;

import com.cburch.logisim.verilog.comp.specs.gatelvl.GateOp;
import com.cburch.logisim.verilog.comp.specs.wordlvl.*;

import java.util.*;

public final class CellType {

    public enum Level { WORD, GATE, MODULE }
    public enum Kind  { UNARY, BINARY, SIMPLE_GATE, COMPLEX_GATE, FLIP_FLOP, REGISTER, MULTIPLEXER, MEMORY, OTHER }

    private final String typeId;    // ID de Yosys
    private final Level level;      // Abstraccion
    private final Kind kind;        // Clasificación

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

    private static final Set<String> GATE_FLIP_FLOP = Set.of(
            "$_ALDFFE_NNN_", "$_ALDFFE_NNP_", "$_ALDFFE_NPN_", "$_ALDFFE_NPP_", "$_ALDFFE_PNN_",
            "$_ALDFFE_PNP_", "$_ALDFFE_PPN_", "$_ALDFFE_PPP_", "$_ALDFF_NN_", "$_ALDFF_NP_", "$_ALDFF_PN_",
            "$_ALDFF_PP_", "$_DFFE_NN0N_", "$_DFFE_NN0P_", "$_DFFE_NN1N_", "$_DFFE_NN1P_", "$_DFFE_NN_",
            "$_DFFE_NP0N_", "$_DFFE_NP0P_", "$_DFFE_NP1N_", "$_DFFE_NP1P_", "$_DFFE_NP_", "$_DFFE_PN0N_",
            "$_DFFE_PN0P_", "$_DFFE_PN1N_", "$_DFFE_PN1P_", "$_DFFE_PN_", "$_DFFE_PP0N_", "$_DFFE_PP0P_",
            "$_DFFE_PP1N_", "$_DFFE_PP1P_", "$_DFFE_PP_", "$_DFFSRE_NNNN_", "$_DFFSRE_NNNP_",
            "$_DFFSRE_NNPN_","$_DFFSRE_NNPP_","$_DFFSRE_NPNN_","$_DFFSRE_NPNP_","$_DFFSRE_NPPN_",
            "$_DFFSRE_NPPP_","$_DFFSRE_PNNN_","$_DFFSRE_PNNP_","$_DFFSRE_PNPN_","$_DFFSRE_PNPP_",
            "$_DFFSRE_PPNN_","$_DFFSRE_PPNP_","$_DFFSRE_PPPN_","$_DFFSRE_PPPP_","$_DFFSR_NNN_",
            "$_DFFSR_NNP_","$_DFFSR_NPN_","$_DFFSR_NPP_","$_DFFSR_PNN_","$_DFFSR_PNP_","$_DFFSR_PPN_",
            "$_DFFSR_PPP_","$_DFF_NN0_","$_DFF_NN1_","$_DFF_NP0_","$_DFF_NP1_","$_DFF_N_","$_DFF_PN0_",
            "$_DFF_PN1_","$_DFF_PP0_","$_DFF_PP1_","$_DFF_P_","$_FF_","$_SDFFCE_NN0N_","$_SDFFCE_NN0P_",
            "$_SDFFCE_NN1N_","$_SDFFCE_NN1P_","$_SDFFCE_NP0N_","$_SDFFCE_NP0P_","$_SDFFCE_NP1N_",
            "$_SDFFCE_NP1P_","$_SDFFCE_PN0N_","$_SDFFCE_PN0P_","$_SDFFCE_PN1N_","$_SDFFCE_PN1P_",
            "$_SDFFCE_PP0N_","$_SDFFCE_PP0P_","$_SDFFCE_PP1N_","$_SDFFCE_PP1P_","$_SDFFE_NN0N_",
            "$_SDFFE_NN0P_","$_SDFFE_NN1N_","$_SDFFE_NN1P_","$_SDFFE_NP0N_","$_SDFFE_NP0P_","$_SDFFE_NP1N_",
            "$_SDFFE_NP1P_","$_SDFFE_PN0N_","$_SDFFE_PN0P_","$_SDFFE_PN1N_","$_SDFFE_PN1P_","$_SDFFE_PP0N_",
            "$_SDFFE_PP0P_","$_SDFFE_PP1N_","$_SDFFE_PP1P_","$_SDFF_NN0_","$_SDFF_NN1_","$_SDFF_NP0_",
            "$_SDFF_NP1_","$_SDFF_PN0_","$_SDFF_PN1_","$_SDFF_PP0_","$_SDFF_PP1_"
    );

    /** Clasificador principal: de typeId (Yosys) a CellType */
    public static CellType fromYosys(String typeId) {
        if (typeId == null || typeId.isEmpty()) {
            return new CellType("<unknown>", Level.MODULE, Kind.OTHER);
        }

        // Gate-level (prefijo $_)
        if (typeId.startsWith("$_")) {
            // ¿es un gate simple/combined/mux-family?
            if (GateOp.isGateTypeId(typeId)) {
                GateOp op = GateOp.fromYosys(typeId);
                return switch (op.category()) {
                    case SIMPLE      -> new CellType(typeId, Level.GATE, Kind.SIMPLE_GATE);
                    case COMBINED    -> new CellType(typeId, Level.GATE, Kind.COMPLEX_GATE);
                    case MUX_FAMILY  -> new CellType(typeId, Level.GATE, Kind.MULTIPLEXER);
                };
            }
            // ¿flip-flop gate-level?
            // Por simplicidad: gates desconocidos
            return new CellType(typeId, Level.GATE, Kind.OTHER);
        }

        // Word-level (prefijo $ pero no $_)
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
            // Word-level desconocido
            return new CellType(typeId, Level.WORD, Kind.OTHER);
        }

        // Instancia de módulo del usuario (sin '$')
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
