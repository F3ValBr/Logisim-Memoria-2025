package com.cburch.logisim.std.yosys;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.std.PortMapRegister;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.verilog.std.BuiltinPortMaps;

import java.util.LinkedHashMap;
import java.util.Map;

public class YosysComponentsPortMapRegister implements PortMapRegister {
    @Override
    public void register(LogisimFile lf) {
        Library YCLib = lf.getLibrary(YosysComponent.LIB_NAME);
        if (YCLib == null) return;

        BuiltinPortMaps.registerByName(YCLib.getName(), LogicalNotGate._ID,
                Map.of("A", LogicalNotGate.A, "Y", LogicalNotGate.OUT));
        BuiltinPortMaps.registerByName(YCLib.getName(), LogicalAndGate._ID,
                Map.of("A", LogicalAndGate.A, "B", LogicalAndGate.B, "Y", LogicalAndGate.OUT));
        BuiltinPortMaps.registerByName(YCLib.getName(), LogicalOrGate._ID,
                Map.of("A", LogicalOrGate.A, "B", LogicalOrGate.B, "Y", LogicalOrGate.OUT));
        BuiltinPortMaps.registerByName(YCLib.getName(), Exponent._ID,
                Map.of("A", Exponent.IN0, "B", Exponent.IN1, "Y", Exponent.OUT));
        BuiltinPortMaps.registerByName(YCLib.getName(), DynamicShifter._ID,
                Map.of("A", DynamicShifter.IN_A, "B", DynamicShifter.IN_B, "Y", DynamicShifter.OUT_Y));
        BuiltinPortMaps.registerByName(YCLib.getName(), BitwiseMultiplexer._ID,
                Map.of("A", BitwiseMultiplexer.A, "B", BitwiseMultiplexer.B, "S", BitwiseMultiplexer.S, "Y", BitwiseMultiplexer.Y));
        BuiltinPortMaps.registerByName(YCLib.getName(), BinaryMultiplexer._ID,
                Map.of("A", BinaryMultiplexer.A, "AX", BinaryMultiplexer.A_X, "S", BinaryMultiplexer.S, "Y", BinaryMultiplexer.Y));
        BuiltinPortMaps.registerResolverByName(
                YCLib.getName(),
                PriorityMultiplexer._ID,
                YosysComponentsPortMapRegister::resolvePriMuxPorts
        );
    }

    private static Map<String, Integer> resolvePriMuxPorts(Component comp) {
        // El orden en updatePorts es: A, B, (BX si hasExtra), S, Y
        // Por lo tanto, los índices quedan:
        // hasExtra=false → A=0, B=1, S=2, Y=3
        // hasExtra=true  → A=0, B=1, BX=2, S=3, Y=4

        int pinCount = comp.getEnds().size();
        boolean hasExtra = (pinCount == 5);

        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("A", 0);
        m.put("B", 1);
        if (hasExtra) {
            m.put("BX", 2);          // parte alta del bus B
            m.put("S",  3);
            m.put("Y",  4);
        } else {
            m.put("S",  2);
            m.put("Y",  3);
        }
        return m;
    }
}
