package com.leinardi.plugins.pylint_plugin.actions;

import com.leinardi.plugins.pylint_plugin.PylintTerminal;
import com.leinardi.plugins.pylint_plugin.PylintToolWindowFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

public class PrevViolation extends AnAction implements DumbAware {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(
                PylintToolWindowFactory.PYLINT_PLUGIN_ID);
        if (!tw.isVisible()) {
            tw.show((Runnable) null);
        }
        PylintTerminal terminal = PylintToolWindowFactory.getPylintTerminal(project);
        if (terminal == null) {
            return;
        }
        if (terminal.getRunner().isRunning()) {
            return;
        }
        int current = terminal.getErrorsList().getSelectedIndex();
        if (current > 0) {
            terminal.getErrorsList().setSelectedIndex(current - 1);
        }
    }
}
