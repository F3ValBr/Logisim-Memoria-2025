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
import com.cburch.logisim.verilog.comp.auxiliary.CellType;
import com.cburch.logisim.verilog.comp.auxiliary.PortEndpoint;
import com.cburch.logisim.verilog.comp.specs.CellAttribs;
import com.cburch.logisim.verilog.comp.specs.CellParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.BinaryOpParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.UnaryOpParams;
import com.cburch.logisim.verilog.comp.specs.wordlvl.muxparams.*;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysCellDTO;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysJsonNetlist;
import com.cburch.logisim.verilog.file.jsonhdlr.YosysModuleDTO;
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

        YosysJsonNetlist netlist = YosysJsonNetlist.from(root);
        CellFactoryRegistry registry = new CellFactoryRegistry();

        int total = 0;
        for (YosysModuleDTO mod : (Iterable<YosysModuleDTO>) netlist.modules()::iterator) {
            System.out.println("== Módulo: " + mod.name() + " ==");
            for (YosysCellDTO c : (Iterable<YosysCellDTO>) mod.cells()::iterator) {
                VerilogCell cell = registry.createCell(
                        c.name(),
                        c.typeId(),
                        c.parameters(),
                        c.attributes(),
                        c.portDirections(),
                        c.connections()
                );
                printCellSummary(cell);
                total++;
            }
            System.out.println();
        }
        System.out.println("Total de celdas procesadas: " + total);
    }

    /* =========================
        Helper de impresión
   ========================= */

    private static void printCellSummary(VerilogCell cell) {
        CellType ct = cell.type();

        String ports = cell.getPortNames().stream()
                .sorted()
                .map(p -> p + "[" + cell.portWidth(p) + "]")
                .collect(Collectors.joining(", "));

        // Línea principal
        System.out.println(" - Cell: " + cell.name()
                + " | typeId=" + ct.typeId()
                + " | level=" + ct.level()
                + " | kind=" + ct.kind()
                + " | ports={" + ports + "}"
        );

        // Parameters (genérico)
        String paramsStr = formatParams(cell.params());
        if (!paramsStr.isEmpty()) {
            System.out.println("   · params: " + paramsStr);
        }

        // Attributes (genérico)
        String attribsStr = formatAttribs(cell.attribs());
        if (!attribsStr.isEmpty()) {
            System.out.println("   · attribs: " + attribsStr);
        }

        // (Opcional) Resumen semántico por familia conocida
        printSemanticHint(cell);
    }

    /** Devuelve los parámetros ordenados por clave: key=value, key2=value2… */
    private static String formatParams(CellParams params) {
        if (params == null) return "";
        Map<String, Object> map = params.asMap();
        if (map == null || map.isEmpty()) return "";
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + stringifyParamValue(e.getValue()))
                .collect(Collectors.joining(", "));
    }

    /** Devuelve los atributos ordenados por clave. */
    private static String formatAttribs(CellAttribs attribs) {
        if (attribs == null) return "";
        Map<String, Object> map = attribs.asMap();
        if (map == null || map.isEmpty()) return "";
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + stringifyParamValue(e.getValue()))
                .collect(Collectors.joining(", "));
    }

    /** Convierte un valor a string, tratando números/booleanos y dejando otros tal cual. */
    private static String stringifyParamValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        String s = String.valueOf(v).trim();
        // Acorta binarios larguísimos
        if (s.length() > 64 && isBinaryString(s)) {
            return s.substring(0, 32) + "…(" + s.length() + "b)…" + s.substring(s.length()-8);
        }
        return s;
    }

    private static boolean isBinaryString(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '0' && c != '1') return false;
        }
        return true;
    }

/* =========================
   (Opcional) pista semántica
   ========================= */

    private static void printSemanticHint(VerilogCell cell) {
        CellType ct = cell.type();

        // Si tienes clases de params especializadas, puedes reconocerlas:
        if (cell.params() instanceof UnaryOpParams p) {
            System.out.println("   · unary: A_WIDTH=" + p.aWidth()
                    + ", Y_WIDTH=" + p.yWidth()
                    + ", A_SIGNED=" + p.aSigned());
            return;
        }
        if (cell.params() instanceof BinaryOpParams p) {
            System.out.println("   · binary: A_WIDTH=" + p.aWidth()
                    + ", B_WIDTH=" + p.bWidth()
                    + ", Y_WIDTH=" + p.yWidth()
                    + ", A_SIGNED=" + p.aSigned()
                    + ", B_SIGNED=" + p.bSigned());
            return;
        }
        if (cell.params() instanceof MuxParams p) {
            System.out.println("   · mux: WIDTH=" + p.width() + ", S=1bit");
            return;
        }
        if (cell.params() instanceof PMuxParams p) {
            System.out.println("   · pmux: WIDTH=" + p.width()
                    + ", S_WIDTH=" + p.sWidth()
                    + ", |B|=" + p.bTotalWidth());
            return;
        }
        if (cell.params() instanceof TribufParams p) {
            System.out.println("   · tribuf: WIDTH=" + p.width() + ", EN=1bit");
            return;
        }
        if (cell.params() instanceof BMuxParams p) {
            System.out.println("   · bmux: WIDTH=" + p.width()
                    + ", S_WIDTH=" + p.sWidth()
                    + ", |A|=" + p.aTotalWidth());
            return;
        }
        if (cell.params() instanceof BWMuxParams p) {
            System.out.println("   · bwmux: WIDTH=" + p.width()
                    + ", S_WIDTH=" + p.sWidth());
            return;
        }
        if (cell.params() instanceof DemuxParams p) {
            System.out.println("   · demux: WIDTH=" + p.width()
                    + ", S_WIDTH=" + p.sWidth()
                    + ", |Y|=" + p.yTotalWidth());
            return;
        }

        // Si no hay clases especializadas, puedes usar el Kind del CellType:
        switch (ct.kind()) {
            case UNARY    -> System.out.println("   · unary op");
            case BINARY   -> System.out.println("   · binary op");
            case MULTIPLEXER -> System.out.println("   · mux family");
            case REGISTER -> System.out.println("   · register");
            case MEMORY   -> System.out.println("   · memory");
            case SIMPLE_GATE -> System.out.println("   · simple gate");
            case COMPLEX_GATE -> System.out.println("   · complex gate");
            case FLIP_FLOP -> System.out.println("   · flip-flop gate");
            default -> { /* nada */ }
        }
    }



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
