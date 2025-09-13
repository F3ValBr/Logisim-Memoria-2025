package com.cburch.logisim.std.riscv;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class ProcessorMenu implements ActionListener, MenuExtender {
    private final Processor factory;
    private final Instance instance;
    private Project proj;
    private Frame frame;
    private CircuitState circState;
    private JMenuItem edit;
    private JMenuItem clear;
    ProcessorMenu(Processor factory, Instance instance) {
        this.factory = factory;
        this.instance = instance;
    }
    public void configureMenu(JPopupMenu menu, Project proj) {
        this.proj = proj;
        this.frame = proj.getFrame();
        this.circState = proj.getCircuitState();

        Object attrs = instance.getAttributeSet();
        if (attrs instanceof ProcessorAttributes) {
            ((ProcessorAttributes) attrs).setProject(proj);
        }
        boolean enabled= circState!=null;
        edit = createItem(enabled, Strings.get("processorEditMenuItem"));
        clear = createItem(enabled,Strings.get("processorClearMenuItem"));

        menu.addSeparator();
        menu.add(edit);
        menu.add(clear);
    }

    private JMenuItem createItem(boolean enabled, String label) {
        JMenuItem ret = new JMenuItem(label);
        ret.setEnabled(enabled);
        ret.addActionListener(this);
        return ret;
    }
    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        if (src == edit) doEdit();
        else if (src == clear) doClear();
    }

    private void doEdit() {
        ProcessorData s = factory.getData(instance, circState);
        if (s == null) return;
        HexFrame frame = factory.getHexFrame(proj, instance);
        frame.setVisible(true);
        frame.toFront();
    }

    private void doClear() {
        ProcessorData s = factory.getData(instance, circState);
        boolean isAllZero = s.getContents().isClear();
        if (isAllZero) return;

        int choice = JOptionPane.showConfirmDialog(frame,
                Strings.get("ramConfirmClearMsg"),
                Strings.get("ramConfirmClearTitle"),
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            s.getContents().clear();
            factory.clearContentAttributes(instance);
        }
    }
}
