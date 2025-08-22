package com.cburch.logisim.verilog.comp.aux;

import com.cburch.logisim.verilog.comp.VerilogCell;
import com.cburch.logisim.verilog.comp.aux.netconn.Direction;

import java.util.Objects;

public final class PortSignature {
    private final VerilogCell cell;     // Celda a la que pertenece el puerto
    private final String portName;      // Nombre del puerto en la celda
    private final Direction direction;  // INPUT, OUTPUT, INOUT

    public PortSignature(VerilogCell cell, String portName, Direction direction) {
        this.cell = cell;
        this.portName = portName;
        this.direction = direction;
    }

    public VerilogCell cell() {
        return cell;
    }
    public String portName() {
        return portName;
    }
    public Direction direction() {
        return direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortSignature that)) return false;

        return direction == that.direction
                && Objects.equals(cell, that.cell)
                && Objects.equals(portName, that.portName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cell, portName, direction);
    }
}
