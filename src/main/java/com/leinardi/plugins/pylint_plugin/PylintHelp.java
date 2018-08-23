package com.leinardi.plugins.pylint_plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Color;

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
