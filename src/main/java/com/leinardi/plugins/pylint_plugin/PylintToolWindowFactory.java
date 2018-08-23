package com.leinardi.plugins.pylint_plugin;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class PylintToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String DEFAULT_PYLINT_PATH_SUFFIX = "";
    public static final String DEFAULT_PYLINT_COMMAND = "pylint --msg-template='{path}:{line:d}: {C}: [{C}]: {msg} " +
            "({symbol})' .";
    public static final String PYLINT_PLUGIN_ID = "Pylint Terminal";
    public static final HashMap<Project, PylintTerminal> INSTANCES = new HashMap<>();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        PylintTerminal terminal = new PylintTerminal(project);
        terminal.initUI(toolWindow);
        INSTANCES.put(project, terminal);
    }

    public static PylintTerminal getPylintTerminal(Project project) {
        PylintTerminal pylintTerminal = INSTANCES.get(project);
        if (pylintTerminal == null) {
            throw new IllegalStateException("PylintTerminal is null!");
        }
        return pylintTerminal;
    }
}
