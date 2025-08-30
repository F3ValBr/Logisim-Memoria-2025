/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.menu;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;

import com.cburch.logisim.analyze.gui.Analyzer;
import com.cburch.logisim.analyze.gui.AnalyzerManager;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.circuit.AnalyzeException;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.StringUtil;

import com.cburch.logisim.verilog.comp.CellFactoryRegistry;
import com.cburch.logisim.verilog.comp.VerilogCell;
import com.cburch.logisim.verilog.comp.VerilogModuleBuilder;
import com.cburch.logisim.verilog.comp.VerilogModuleImpl;
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.EndpointVisitor;
import com.cburch.logisim.verilog.comp.auxiliary.ModulePort;
import com.cburch.logisim.verilog.comp.auxiliary.NetTraversal;
import com.cburch.logisim.verilog.comp.specs.CellAttribs;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.GenericCellAttribs;
import com.cburch.logisim.verilog.comp.specs.GenericCellParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.BinaryOpParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.UnaryOpParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams.*;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysCellDTO;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysJsonNetlist;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysModuleDTO;
import com.cburch.logisim.verilog.layout.ModuleNetIndex;
import com.fasterxml.jackson.databind.JsonNode;

public class ProjectCircuitActions {
	private ProjectCircuitActions() { }
	
	public static void doAddCircuit(Project proj) {
		String name = promptForCircuitName(proj.getFrame(), proj.getLogisimFile(), "");
		if (name != null) {
			Circuit circuit = new Circuit(name);
			proj.doAction(LogisimFileActions.addCircuit(circuit));
			proj.setCurrentCircuit(circuit);
		}
	}

	/**
	 * Imports a circuit from a JSON file, which is expected to be in JSON format from YoSYS synthesis.
	 * The actual import logic is not implemented in this stub method.
	 *
	 * @param proj the project to which the circuit will be imported
	 */
    public static void doImportJsonVerilog(Project proj) {
        System.out.println("Importing JSON Verilog...");
        JsonNode root = proj.getLogisimFile().getLoader().JSONImportChooser(proj.getFrame());
        if (root == null) {
            System.out.println("Import cancelled.");
            return;
        }

        // 0) Abrimos el netlist y preparamos factories + builder
        YosysJsonNetlist netlist = YosysJsonNetlist.from(root);
        CellFactoryRegistry registry = new CellFactoryRegistry();
        VerilogModuleBuilder builder = new VerilogModuleBuilder(registry);

        int totalCells = 0;

        // 1) Recorremos módulos
        for (YosysModuleDTO dto : (Iterable<YosysModuleDTO>) netlist.modules()::iterator) {
            VerilogModuleImpl mod = builder.buildModule(dto);
            System.out.println("== Módulo: " + mod.name() + " ==");

            // Puertos
            printModulePorts(mod);

            // Celdas
            for (VerilogCell cell : mod.cells()) {
                printCellSummary(cell);
                totalCells++;
            }

            // Nets internos (usando ModuleNetIndex)
            ModuleNetIndex netIndex = builder.buildNetIndex(mod);
            printNets(mod, netIndex);

            System.out.println();
        }

        System.out.println("Total de celdas procesadas: " + totalCells);
    }


    /* =========================
       Impresión de puertos
       ========================= */
    private static void printModulePorts(VerilogModuleImpl mod) {
        if (mod.ports().isEmpty()) {
            System.out.println("  (sin puertos)");
            return;
        }
        System.out.println("Puertos (" + mod.ports().size() + "):");
        for (ModulePort p : mod.ports()) {
            System.out.print("  - " + p.name() + ":" + p.direction() + "[" + p.width() + "]");
            // imprime breve vista de los primeros bits (netId o constante)
            int preview = Math.min(8, p.width());
            System.out.print(" bits[0.." + (preview - 1) + "]={");
            for (int i = 0; i < preview; i++) {
                int id = p.netIdAt(i);
                String tok = (id >= 0) ? String.valueOf(id)
                        : (id == ModulePort.CONST_0 ? "0"
                        : (id == ModulePort.CONST_1 ? "1" : "x"));
                if (i > 0) System.out.print(", ");
                System.out.print(tok);
            }
            if (p.width() > preview) System.out.print(", ...");
            System.out.println("}");
        }
    }

    /* =========================
       Impresión de celdas
       ========================= */
    private static void printCellSummary(VerilogCell cell) {
        CellType ct = cell.type();

        // nombres y widths por puerto (derivado de endpoints)
        String ports = cell.getPortNames().stream()
                .sorted()
                .map(p -> p + "[" + cell.portWidth(p) + "]")
                .collect(java.util.stream.Collectors.joining(", "));

        System.out.println(" - Cell: " + cell.name()
                + " | typeId=" + ct.typeId()
                + " | level=" + ct.level()
                + " | kind=" + ct.kind()
                + " | ports={" + ports + "}");

        // parámetros y atributos (si existen)
        String paramsStr = paramsToString(cell.params());
        if (!paramsStr.isEmpty()) {
            System.out.println("     params: " + paramsStr);
        }
        String attribsStr = attribsToString(cell.attribs());
        if (!attribsStr.isEmpty()) {
            System.out.println("     attribs: " + attribsStr);
        }
    }

    private static void printNets(VerilogModuleImpl mod, ModuleNetIndex netIndex) {
        System.out.println("Nets internos: " + netIndex.netIds().size());

        for (int netId : netIndex.netIds()) {
            System.out.println(" - Net " + netId + ":");

            NetTraversal.visitNet(netIndex, netId, new EndpointVisitor() {
                @Override public void topPort(int portIdx, int bitIdx) {
                    ModulePort p = mod.ports().get(portIdx);
                    System.out.println("    top: " + p.name() + "[" + bitIdx + "] (" + p.direction() + ")");
                }

                @Override public void cellBit(int cellIdx, int bitIdx) {
                    VerilogCell cell = mod.cells().get(cellIdx);
                    // aquí no tengo portName directo, pero sí puedes encontrarlo via endpoints:
                    // busca el endpoint con ese bitIdx (puedes mejorar esto luego con un PinLocator)
                    System.out.println("    cell: " + cell.name() + " bit[" + bitIdx + "] type=" + cell.type().typeId());
                }
            });
        }
    }


    /* =========================
       Helpers para mostrar params/attribs
       ========================= */
    private static String paramsToString(CellParams p) {
        if (p == null) return "";
        // Si es genérico, imprime el mapa crudo:
        if (p instanceof GenericCellParams g) {
            return g.asMap().toString();
        }
        // Si tienes clases específicas, dales un toString útil;
        // por defecto, usa toString():
        return p.toString();
    }

    private static String attribsToString(CellAttribs a) {
        if (a == null) return "";
        if (a instanceof GenericCellAttribs g) {
            return g.asMap().toString();
        }
        return a.toString();
    }
    // fin


	private static String promptForCircuitName(JFrame frame,
			Library lib, String initialValue) {
		JLabel label = new JLabel(Strings.get("circuitNamePrompt"));
		final JTextField field = new JTextField(15);
		field.setText(initialValue);
		JLabel error = new JLabel(" ");
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		JPanel strut = new JPanel(null);
		strut.setPreferredSize(new Dimension(3 * field.getPreferredSize().width / 2, 0));
		JPanel panel = new JPanel(gb);
		gc.gridx = 0;
		gc.gridy = GridBagConstraints.RELATIVE;
		gc.weightx = 1.0;
		gc.fill = GridBagConstraints.NONE;
		gc.anchor = GridBagConstraints.LINE_START;
		gb.setConstraints(label, gc); panel.add(label);
		gb.setConstraints(field, gc); panel.add(field);
		gb.setConstraints(error, gc); panel.add(error);
		gb.setConstraints(strut, gc); panel.add(strut);
		JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		pane.setInitialValue(field);
		JDialog dlog = pane.createDialog(frame, Strings.get("circuitNameDialogTitle"));
		dlog.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent arg0) {
				field.requestFocus();
			}

			public void windowLostFocus(WindowEvent arg0) { }
		});
		
		while (true) {
			field.selectAll();
			dlog.pack();
			dlog.setVisible(true);
			field.requestFocusInWindow();
			Object action = pane.getValue();
			if (action == null || !(action instanceof Integer)
					|| ((Integer) action).intValue() != JOptionPane.OK_OPTION) {
				return null;
			}

			String name = field.getText().trim();
			if (name.equals("")) {
				error.setText(Strings.get("circuitNameMissingError"));
			} else {
				if (lib.getTool(name) == null) {
					return name;
				} else {
					error.setText(Strings.get("circuitNameDuplicateError"));
				}
			}
		}
	}

	public static void doMoveCircuit(Project proj, Circuit cur, int delta) {
		AddTool tool = proj.getLogisimFile().getAddTool(cur);
		if (tool != null) {
			int oldPos = proj.getLogisimFile().getCircuits().indexOf(cur);
			int newPos = oldPos + delta;
			int toolsCount = proj.getLogisimFile().getTools().size();
			if (newPos >= 0 && newPos < toolsCount) {
				proj.doAction(LogisimFileActions.moveCircuit(tool, newPos));
			}
		}
	}

	public static void doSetAsMainCircuit(Project proj, Circuit circuit) {
		proj.doAction(LogisimFileActions.setMainCircuit(circuit));
	}

	public static void doRemoveCircuit(Project proj, Circuit circuit) {
		if (proj.getLogisimFile().getTools().size() == 1) {
			JOptionPane.showMessageDialog(proj.getFrame(),
					Strings.get("circuitRemoveLastError"),
					Strings.get("circuitRemoveErrorTitle"),
					JOptionPane.ERROR_MESSAGE);
		} else if (!proj.getDependencies().canRemove(circuit)) {
			JOptionPane.showMessageDialog(proj.getFrame(),
				Strings.get("circuitRemoveUsedError"),
				Strings.get("circuitRemoveErrorTitle"),
				JOptionPane.ERROR_MESSAGE);
		} else {
			proj.doAction(LogisimFileActions.removeCircuit(circuit));
		}
	}
	
	public static void doAnalyze(Project proj, Circuit circuit) {
		Map<Instance, String> pinNames = Analyze.getPinLabels(circuit);
		ArrayList<String> inputNames = new ArrayList<String>();
		ArrayList<String> outputNames = new ArrayList<String>();
		for (Map.Entry<Instance, String> entry : pinNames.entrySet()) {
			Instance pin = entry.getKey();
			boolean isInput = Pin.FACTORY.isInputPin(pin);
			if (isInput) {
				inputNames.add(entry.getValue());
			} else {
				outputNames.add(entry.getValue());
			}
			if (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1) {
				if (isInput) {
					analyzeError(proj, Strings.get("analyzeMultibitInputError"));
				} else {
					analyzeError(proj, Strings.get("analyzeMultibitOutputError"));
				}
				return;
			}
		}
		if (inputNames.size() > AnalyzerModel.MAX_INPUTS) {
			analyzeError(proj, StringUtil.format(Strings.get("analyzeTooManyInputsError"),
					"" + AnalyzerModel.MAX_INPUTS));
			return;
		}
		if (outputNames.size() > AnalyzerModel.MAX_OUTPUTS) {
			analyzeError(proj, StringUtil.format(Strings.get("analyzeTooManyOutputsError"),
					"" + AnalyzerModel.MAX_OUTPUTS));
			return;
		}
		
		Analyzer analyzer = AnalyzerManager.getAnalyzer();
		analyzer.getModel().setCurrentCircuit(proj, circuit);
		configureAnalyzer(proj, circuit, analyzer, pinNames, inputNames, outputNames);
		analyzer.setVisible(true);
		analyzer.toFront();
	}
	
	private static void configureAnalyzer(Project proj, Circuit circuit,
			Analyzer analyzer, Map<Instance, String> pinNames,
			ArrayList<String> inputNames, ArrayList<String> outputNames) {
		analyzer.getModel().setVariables(inputNames, outputNames);
		
		// If there are no inputs, we stop with that tab selected
		if (inputNames.size() == 0) {
			analyzer.setSelectedTab(Analyzer.INPUTS_TAB);
			return;
		}
		
		// If there are no outputs, we stop with that tab selected
		if (outputNames.size() == 0) {
			analyzer.setSelectedTab(Analyzer.OUTPUTS_TAB);
			return;
		}
		
		// Attempt to show the corresponding expression
		try {
			Analyze.computeExpression(analyzer.getModel(), circuit, pinNames);
			analyzer.setSelectedTab(Analyzer.EXPRESSION_TAB);
			return;
		} catch (AnalyzeException ex) {
			JOptionPane.showMessageDialog(proj.getFrame(), ex.getMessage(),
					Strings.get("analyzeNoExpressionTitle"),
					JOptionPane.INFORMATION_MESSAGE);
		}
		
		// As a backup measure, we compute a truth table.
		Analyze.computeTable(analyzer.getModel(), proj, circuit, pinNames);
		analyzer.setSelectedTab(Analyzer.TABLE_TAB);
	}
		
	private static void analyzeError(Project proj, String message) {
		JOptionPane.showMessageDialog(proj.getFrame(), message,
			Strings.get("analyzeErrorTitle"),
			JOptionPane.ERROR_MESSAGE);
		return;
	}
}
