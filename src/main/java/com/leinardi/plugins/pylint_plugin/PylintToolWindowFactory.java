package com.leinardi.plugins.pylint_plugin;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;


public class PylintToolWindowFactory implements ToolWindowFactory, DumbAware {
    final public static String DEFAULT_PYLINT_PATH_SUFFIX = "";
    final public static String DEFAULT_PYLINT_COMMAND = "pylint --msg-template='{path}:{line:d}: {C}: [{C}]: {msg} ({symbol})' .";
    final public static String PYLINT_PLUGIN_ID = "Pylint Terminal";
    final public static HashMap<Project, PylintTerminal> instances = new HashMap<>();

    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        PylintTerminal terminal = new PylintTerminal(project);
        terminal.initUI(toolWindow);
        instances.put(project, terminal);
    }

    public static PylintTerminal getPylintTerminal(Project project) {
        return instances.get(project);
    }
}
