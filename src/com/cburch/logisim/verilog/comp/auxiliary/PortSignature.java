package com.cburch.logisim.verilog.comp.auxiliary;

import com.cburch.logisim.verilog.comp.VerilogCell;
import com.cburch.logisim.verilog.comp.auxiliary.netconn.Direction;

import java.util.Objects;

/**
 * Represents the signature of a port in a Verilog cell, including the cell it belongs to,
 * the port name, and its direction (INPUT, OUTPUT, INOUT).
 */
public final class PortSignature {
    private final VerilogCell cell;     // Celda a la que pertenece el puerto
    private final String portName;      // Nombre del puerto en la celda
    private final Direction direction;  // INPUT, OUTPUT, INOUT

    /**
     * Constructor for PortSignature
     *
     * @param cell      The VerilogCell to which the port belongs
     * @param portName  The name of the port within the cell
     * @param direction The direction of the port (INPUT, OUTPUT, INOUT)
     */
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
