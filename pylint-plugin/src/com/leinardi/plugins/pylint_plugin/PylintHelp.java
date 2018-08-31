package com.leinardi.plugins.pylint_plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PylintHelp extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextPane textPane;

    public PylintHelp() {
        setContentPane(contentPane);
        setModal(false);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> dispose());
    }

    public static void main(Project project) {
        PylintHelp dialog = new PylintHelp();
        dialog.pack();
        dialog.setSize(600, 400);
        JFrame frame = WindowManager.getInstance().getFrame(project);
        dialog.setLocationRelativeTo(frame);
        dialog.textPane.setCaretPosition(0);
        dialog.textPane.setForeground(new JBColor(PylintTerminal.BLACK, PylintTerminal.GRAY));
        dialog.textPane.setBackground(new JBColor(new Color(PylintTerminal.WHITE),
                dialog.contentPane.getBackground()));
        dialog.setVisible(true);
    }
}
