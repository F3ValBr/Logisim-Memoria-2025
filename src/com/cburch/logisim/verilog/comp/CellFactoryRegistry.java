package com.cburch.logisim.verilog.comp;

import com.cburch.logisim.verilog.comp.factories.ModuleInstanceFactory;
import com.cburch.logisim.verilog.comp.factories.wordlvl.BinaryOpFactory;
import com.cburch.logisim.verilog.comp.factories.wordlvl.MuxOpFactory;
import com.cburch.logisim.verilog.comp.factories.wordlvl.UnaryOpFactory;
import com.cburch.logisim.verilog.comp.specs.wordlvl.BinaryOp;
import com.cburch.logisim.verilog.comp.specs.wordlvl.MuxOp;
import com.cburch.logisim.verilog.comp.specs.wordlvl.UnaryOp;

import java.util.*;

public class CellFactoryRegistry {
    private final Map<String, VerilogCellFactory> overrides = new HashMap<>();

    private final VerilogCellFactory unaryFactory  = new UnaryOpFactory();
    private final VerilogCellFactory binaryFactory = new BinaryOpFactory();
    private final VerilogCellFactory muxFactory    = new MuxOpFactory();
    //private final VerilogCellFactory gateFactory   = new GateOpFactory();
    private final VerilogCellFactory moduleFactory = new ModuleInstanceFactory();

    public void register(String typeId, VerilogCellFactory factory) {
        overrides.put(Objects.requireNonNull(typeId), Objects.requireNonNull(factory));
    }

    public VerilogCell createCell(String name, String typeId,
                                  Map<String,String> parameters,
                                  Map<String,Object> attributes,
                                  Map<String,String> portDirections,
                                  Map<String,List<Object>> connections) {

        // 0) Override explícito
        VerilogCellFactory f = overrides.get(typeId);
        if (f != null) {
            return f.create(name, typeId, parameters, attributes, portDirections, connections);
        }

        // 1) Si viene marcado explícitamente como módulo no derivado → módulo
        if (isModuleNotDerived(attributes)) {
            return moduleFactory.create(name, typeId, parameters, attributes, portDirections, connections);
        }

        // 2) Si no tiene typeId o está vacío → tratar como módulo
        if (typeId == null || typeId.isBlank()) {
            return moduleFactory.create(name, "<unknown>", parameters, attributes, portDirections, connections);
        }

        // 3) Gate-level conocidos ($_...)
        /**
        if (typeId.startsWith("$_")) {
            if (GateOp.isGateTypeId(typeId)) {
                return gateFactory.create(name, typeId, parameters, attributes, portDirections, connections);
            }
            // gate desconocido → MÓDULO
            return moduleFactory.create(name, typeId, parameters, attributes, portDirections, connections);
        }*/

        // 4) Word-level ($...)
        if (typeId.startsWith("$")) {
            if (UnaryOp.isUnaryTypeId(typeId)) {
                return unaryFactory.create(name, typeId, parameters, attributes, portDirections, connections);
            }
            if (BinaryOp.isBinaryTypeId(typeId)) {
                return binaryFactory.create(name, typeId, parameters, attributes, portDirections, connections);
            }
            if (MuxOp.isMuxTypeId(typeId)) {
                return muxFactory.create(name, typeId, parameters, attributes, portDirections, connections);
            }
            // word-level desconocido → MÓDULO
            return moduleFactory.create(name, typeId, parameters, attributes, portDirections, connections);
        }

        // 5) Sin '$' → instancia de módulo de usuario
        return moduleFactory.create(name, typeId, parameters, attributes, portDirections, connections);
    }

    /* ===== Helpers ===== */

    private boolean isModuleNotDerived(Map<String,Object> attrs) {
        if (attrs == null) return false;
        Object v = attrs.get("module_not_derived");
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n)  return n.longValue() != 0L;
        String s = String.valueOf(v).trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }
}
