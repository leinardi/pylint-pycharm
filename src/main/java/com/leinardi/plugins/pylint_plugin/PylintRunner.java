package com.leinardi.plugins.pylint_plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.leinardi.plugins.pylint_plugin.model.PylintResult;
import com.leinardi.plugins.pylint_plugin.model.PylintViolation;

import javax.swing.JList;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class PylintRunner {

    private JList<PylintViolation> display;
    private Project project;
    private boolean isRunning;

    public PylintRunner(JList<PylintViolation> display, Project project) {
        this.display = display;
        this.project = project;
        this.isRunning = false;
    }

    public PylintResult runPylint() {
        Process process;
        String directory = project.getBaseDir().getPath();
        PylintConfigService mConfig = PylintConfigService.getInstance(project);

        ProcessBuilder pbuilder = new ProcessBuilder();
        Map<String, String> envProcess = pbuilder.environment();
        Map<String, String> env = System.getenv();

        envProcess.putAll(env);
        String extraPath = mConfig.getPathSuffix();
        if (extraPath == null) {  // config deleted
            extraPath = PylintToolWindowFactory.DEFAULT_PYLINT_PATH_SUFFIX;
        }
        if (!extraPath.equals("")) {
            envProcess.put("PATH", envProcess.get("PATH") + File.pathSeparator + extraPath);
        }
        String pylintCommand = mConfig.getExecutableName();
        if ((pylintCommand == null) || (pylintCommand.equals(""))) {
            pylintCommand = PylintToolWindowFactory.DEFAULT_PYLINT_COMMAND;
        }
        pbuilder.command("/bin/bash", "-c", pylintCommand);
        pbuilder.redirectErrorStream(true);
        pbuilder.redirectInput(new File("/dev/null"));
        this.isRunning = true;
        try {
            process = pbuilder.directory(new File(directory)).start();
        } catch (IOException e) {
            ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(project, e.getMessage(),
                    "Plugin Exception:", Messages.getErrorIcon()));
            this.isRunning = false;
            return null;
        }
        ArrayList<PylintViolation> lines = new ArrayList<>();
        ArrayList<PylintViolation> debug = new ArrayList<>();
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        process.getInputStream()));
        PylintViolation[] data;
        int violationsCount = 0;
        String rating = "";
        try {
            String line;
            while ((line = br.readLine()) != null) {
                if (addIfViolation(line, lines)) {
                    violationsCount++;
                } else if (line.matches(PylintTerminal.RATING_RE)) {
                    rating = line;
                } else {
                    debug.add(new PylintViolation(line, PylintViolation.DEBUG));
                }
                data = new PylintViolation[debug.size()];
                data = debug.toArray(data);
                this.display.setListData(data);
                int max = this.display.getModel().getSize();
                this.display.scrollRectToVisible(this.display.getCellBounds(max - 1, max));
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            ApplicationManager.getApplication().invokeLater(() -> Messages.showMessageDialog(project, e.getMessage(),
                    "Plugin Exception:", Messages.getErrorIcon()));
            this.isRunning = false;
            return null;
        }
        Collections.sort(lines, Comparator.comparing(PylintViolation::getFile));
        this.isRunning = false;
        return new PylintResult(process.exitValue(), violationsCount, lines, rating);
    }

    public boolean isRunning() {
        return isRunning;
    }

    private boolean addIfViolation(String line, ArrayList<PylintViolation> lines) {
        if (line.matches(PylintTerminal.FATAL_RE)) {
            lines.add(new PylintViolation(line, PylintViolation.FATAL));
            return true;
        } else if (line.matches(PylintTerminal.ERROR_RE)) {
            lines.add(new PylintViolation(line, PylintViolation.ERROR));
            return true;
        } else if (line.matches(PylintTerminal.WARNING_RE)) {
            lines.add(new PylintViolation(line, PylintViolation.WARNING));
            return true;
        } else if (line.matches(PylintTerminal.CONVENTION_RE)) {
            lines.add(new PylintViolation(line, PylintViolation.CONVENTION));
            return true;
        } else if (line.matches(PylintTerminal.REFACTOR_RE)) {
            lines.add(new PylintViolation(line, PylintViolation.REFACTOR));
            return true;
        }
        return false;
    }
}
