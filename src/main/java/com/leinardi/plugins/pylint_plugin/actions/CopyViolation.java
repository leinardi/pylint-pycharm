package com.leinardi.plugins.pylint_plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.leinardi.plugins.pylint_plugin.PylintTerminal;
import com.leinardi.plugins.pylint_plugin.PylintToolWindowFactory;
import com.leinardi.plugins.pylint_plugin.model.PylintViolation;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class CopyViolation extends AnAction implements DumbAware {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(
                PylintToolWindowFactory.PYLINT_PLUGIN_ID);
        if (!tw.isVisible()) {
            return;
        }
        PylintTerminal terminal = PylintToolWindowFactory.getPylintTerminal(project);
        if (terminal == null) {
            return;
        }
        if (terminal.getRunner().isRunning()) {
            return;
        }
        PylintViolation error = terminal.getErrorsList().getSelectedValue();
        if (error == null) { // no errors
            return;
        }
        if (error.getLevel() == PylintViolation.HEADER) {
            return;
        }
        StringSelection selection = new StringSelection(error.getRaw());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
}
