package com.cburch.logisim.verilog.comp.aux;

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

    // ---- Mapas/sets de soporte ----
    private static final Set<String> WORD_UNARY = Set.of(
            "$buf","$logic_not","$neg","$not","$pos",
            "$reduce_and","$reduce_bool","$reduce_or","$reduce_xnor","$reduce_xor"
    );

    private static final Set<String> WORD_BINARY = Set.of(
            "$add","$and","$bweqx","$div","$divfloor","$eq","$eqx","$ge","$gt","$le",
            "$logic_and","$logic_or","$lt","$mod","$modfloor","$mul","$ne","$nex",
            "$or","$pow","$shift","$shiftx","$shl","$shr","$sshl","$sshr","$sub","$xnor","$xor"
    );

    private static final Set<String> WORD_MULTIPLEXERS = Set.of(
            "$bmux","$bwmux","$demux","$mux","$pmux","$tribuf"
    );

    private static final Set<String> WORD_REGISTERS = Set.of(
            "$adff","$adffe","$adlatch","$aldff","$aldffe","$dff","$dffe","$dffsr",
            "$dffsre","$dlatch","$dlatchsr","$sdff","$sdffce","$sdffe","$sr"
    );

    private static final Set<String> WORD_MEMORIES = Set.of(
            "$mem","$mem_v2","$meminit","$meminit_v2","$memrd","$memrd_v2","$memwr","$memwr_v2"
    );

    private static final Set<String> GATE_SIMPLE = Set.of(
            "$_AND_","$_BUF_","$_MUX_","$_NAND_","$_NOR_","$_NOT_","$_OR_","$_XNOR_","$_XOR_"
    );

    private static final Set<String> GATE_COMPLEX = Set.of(
            "$_ANDNOT_","$_ORNOT_","$_AOI3_","$_AOI4_","$_OAI3_","$_OAI4_",
            "$_NMUX_","$_MUX4_","$_MUX8_","$_MUX16_"
    );

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
            return new CellType("<unknown>", Level.MODULE, Kind.OTHER); // fallback inocuo
        }

        // 1) Gate-level (prefijo $_)
        if (typeId.startsWith("$_")) {
            if (GATE_SIMPLE.contains(typeId)) {
                return new CellType(typeId, Level.GATE, Kind.SIMPLE_GATE);
            }
            if (GATE_COMPLEX.contains(typeId)) {
                return new CellType(typeId, Level.GATE, Kind.COMPLEX_GATE);
            }
            if (GATE_FLIP_FLOP.contains(typeId)) {
                return new CellType(typeId, Level.GATE, Kind.FLIP_FLOP);
            }
            // Gate-level desconocido pero sigue siendo gate
            return new CellType(typeId, Level.GATE, Kind.OTHER);
        }

        // 2) Word-level (prefijo $ pero no $_)
        if (typeId.startsWith("$")) {
            if (WORD_UNARY.contains(typeId))  return new CellType(typeId, Level.WORD, Kind.UNARY);
            if (WORD_BINARY.contains(typeId)) return new CellType(typeId, Level.WORD, Kind.BINARY);
            if (WORD_MULTIPLEXERS.contains(typeId)) return new CellType(typeId, Level.WORD, Kind.MULTIPLEXER);
            if (WORD_REGISTERS.contains(typeId))   return new CellType(typeId, Level.WORD, Kind.REGISTER);
            if (WORD_MEMORIES.contains(typeId))    return new CellType(typeId, Level.WORD, Kind.MEMORY);
            // Word-level desconocido
            return new CellType(typeId, Level.WORD, Kind.OTHER);
        }

        // 3) Si no empieza con '$' → normalmente instancia de módulo del usuario
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
