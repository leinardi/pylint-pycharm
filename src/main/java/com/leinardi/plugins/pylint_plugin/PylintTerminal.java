package com.leinardi.plugins.pylint_plugin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.leinardi.plugins.pylint_plugin.model.PylintResult;
import com.leinardi.plugins.pylint_plugin.model.PylintViolation;
import icons.PylintIcons;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.max;

public class PylintTerminal {
    static final String REFACTOR_RE = ".+: R:.+";
    static final String CONVENTION_RE = ".+: C:.+";
    static final String WARNING_RE = ".+: W:.+";
    static final String ERROR_RE = ".+: E:.+";
    static final String FATAL_RE = ".+: F:.+";
    static final String RATING_RE = "^Your code has been rated.*";

    static final int GRAY = 12632256;
    static final int DARK_GRAY = 7368816;
    static final int LIGHT_GREEN = 13500365;
    static final int LIGHT_RED = 16764365;
    static final int BLACK = 0;
    static final int WHITE = 16777215;

    private JPanel pylintToolWindowContent;
    private JBList<PylintViolation> errorsList;
    private JTextField pylintStatus;
    private JButton pylintRun;
    private JScrollPane scroll;
    private int rightIndex;
    private Project project;
    private ListCellRenderer defaultRenderer;
    private ListCellRenderer pylintRenderer;
    private PylintRunner runner;
    private ArrayList<String> errorFiles;
    private Set<String> collapsed;
    private HashMap<String, ArrayList<PylintViolation>> errorMap;

    public PylintTerminal(Project project) {
        this.project = project;
    }

    public PylintRunner getRunner() {
        return runner;
    }

    public JBList<PylintViolation> getErrorsList() {
        return errorsList;
    }

    public void toggleExpand(PylintViolation error) {
        String file = error.getFile();
        if (collapsed.contains(file)) {
            collapsed.remove(file);
        } else {
            collapsed.add(file);
        }
    }

    public void initUI(ToolWindow toolWindow) {
        errorsList.getEmptyText().setText("");
        errorsList.setListData(new PylintViolation[]{});
        runner = new PylintRunner(errorsList, project);
        rightIndex = 0;
        // List popup menu.

        JBPopupMenu popup = new JBPopupMenu();
        JBMenuItem gotoItem = new JBMenuItem("Go to error");
        gotoItem.addActionListener(e -> {
            int tot = PylintTerminal.this.errorsList.getModel().getSize();
            int right = PylintTerminal.this.rightIndex;
            int index = PylintTerminal.this.errorsList.getSelectedIndex();
            if ((right >= 0) & (right < tot)) {
                PylintTerminal.this.errorsList.setSelectedIndex(right);
                // If it was already selected, we need to trigger this manually.
                if (right == index) {
                    PylintTerminal.this.openError(index);
                }
            }
        });
        gotoItem.setIcon(AllIcons.Debugger.Actions.Force_run_to_cursor);
        gotoItem.setDisabledIcon(AllIcons.Debugger.Actions.Force_run_to_cursor);
        popup.add(gotoItem);
        JBMenuItem copyItem = new JBMenuItem("Copy error text");
        copyItem.addActionListener(e -> {
            PylintViolation error = PylintTerminal.this.errorsList.getModel().getElementAt(
                    PylintTerminal.this.rightIndex);
            StringSelection selection = new StringSelection(error.getRaw());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });
        copyItem.setIcon(AllIcons.Actions.Copy);
        copyItem.setDisabledIcon(AllIcons.Actions.Copy);
        popup.add(copyItem);
        JBMenuItem copyAllItem = new JBMenuItem("Copy all errors");
        copyAllItem.addActionListener(e -> {
            ArrayList<String> alle = new ArrayList<>();
            int size = PylintTerminal.this.errorsList.getModel().getSize();
            if (size == 0) {
                return;
            }
            for (int i = 0; i < size; i++) {
                PylintViolation err = PylintTerminal.this.errorsList.getModel().getElementAt(i);
                if (err.getLevel() == PylintViolation.HEADER) {
                    continue;
                }
                alle.add(err.getRaw());
            }
            String error = String.join("\n", alle);
            StringSelection selection = new StringSelection(error);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });
        popup.add(copyAllItem);
        JBMenuItem expandItem = new JBMenuItem("Expand");
        expandItem.addActionListener(e -> {
            PylintViolation error = PylintTerminal.this.errorsList.getModel().getElementAt(
                    PylintTerminal.this.rightIndex);
            if (error.getLevel() == PylintViolation.HEADER) {
                PylintTerminal.this.toggleExpand(error);
                PylintTerminal.this.renderList();
                PylintTerminal.this.errorsList.setSelectedIndex(PylintTerminal.this.rightIndex);
            }
        });
        popup.add(expandItem);
        JBMenuItem helpItem = new JBMenuItem("Help");
        helpItem.addActionListener(e -> PylintHelp.main(project));
        popup.add(helpItem);
        JSeparator sep = new JSeparator();
        popup.add(sep);
        JBMenuItem configItem = new JBMenuItem("Configure plugin...");
        configItem.addActionListener(e -> {
            PylintConfig dialog = new PylintConfig(project);
            dialog.show();
        });
        popup.add(configItem);

        // List selection listener.

        errorsList.addListSelectionListener(e -> {
            int index = PylintTerminal.this.errorsList.getSelectedIndex();
            Rectangle rect = PylintTerminal.this.errorsList.getCellBounds(index, index);
            if (rect != null) {
                PylintTerminal.this.errorsList.scrollRectToVisible(rect);
            }
            PylintTerminal.this.openError(index);
        });

        // List mouse listener.

        errorsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = PylintTerminal.this.errorsList.locationToIndex(e.getPoint());
                if ((e.getButton() == MouseEvent.BUTTON2) |
                        (e.getButton() == MouseEvent.BUTTON3) | (e.isControlDown())) {
                    boolean active = !(PylintTerminal.this.runner.isRunning());
                    boolean isError = false;
                    boolean isExpanded = false;
                    boolean isHeader = false;
                    if (index >= 0) {
                        PylintViolation error = PylintTerminal.this.errorsList.getModel().getElementAt(index);
                        isError = error.isViolation();
                        isExpanded = !error.isCollapsed();
                        isHeader = error.getLevel() == PylintViolation.HEADER;
                    }
                    gotoItem.setEnabled(active & isError);
                    gotoItem.updateUI();
                    copyItem.setEnabled(active & !isHeader & (index >= 0));
                    copyItem.updateUI();
                    copyAllItem.setEnabled(active);
                    copyAllItem.updateUI();
                    expandItem.setEnabled(active & isHeader);
                    expandItem.setText(isExpanded ? "Collapse" : "Expand");
                    expandItem.updateUI();
                    configItem.updateUI();
                    helpItem.updateUI();
                    PylintTerminal.this.rightIndex = index;
                    popup.updateUI();
                    popup.show(e.getComponent(), e.getX(), e.getY());
                    return;
                }
                if (e.isAltDown()) {
                    String error = PylintTerminal.this.errorsList.getModel()
                            .getElementAt(index).getMessage();
                    Pattern http = Pattern.compile("http://\\S+");  // TODO: Use better regex.
                    Matcher matcher = http.matcher(error);
                    if (matcher.find()) {
                        String link = error.substring(matcher.start(0), matcher.end(0));
                        try {
                            Desktop.getDesktop().browse(new URL(link).toURI());
                        } catch (URISyntaxException | IOException excep) {
                            Messages.showMessageDialog(project, excep.getMessage(),
                                    "Plugin Exception:", Messages.getErrorIcon());
                        }
                    }
                    return;
                }
                if (e.getClickCount() >= 1) {
                    if (index >= 0) {
                        PylintViolation error = PylintTerminal.this.errorsList.getModel().getElementAt(index);
                        boolean expandable = (error.getLevel() == PylintViolation.HEADER);
                        if (expandable) {
                            PylintTerminal.this.toggleExpand(error);
                            PylintTerminal.this.renderList();
                            PylintTerminal.this.errorsList.setSelectedIndex(index);
                        } else {
                            int old = PylintTerminal.this.errorsList.getSelectedIndex();
                            if (old == index) {
                                // manually trigger if selection didn't change
                                PylintTerminal.this.openError(index);
                            } else {
                                PylintTerminal.this.errorsList.setSelectedIndex(index);
                            }
                        }
                    }

                }
            }
        });

        // Final strokes.

        pylintRun.addActionListener(e -> PylintTerminal.this.runPylintDaemonUIWrapper());
        pylintRun.setIcon(PylintIcons.PYLINT_SMALL);
        pylintRenderer = new PylintCellRenderer();
        defaultRenderer = errorsList.getCellRenderer();

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(pylintToolWindowContent, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public void runPylintDaemonUIWrapper() {

        setWaiting();
        FileDocumentManager.getInstance().saveAllDocuments();
        // Invoke pylint daemon runner script in a sub-thread,
        // it looks like UI is blocked on it otherwise.
        Executors.newSingleThreadExecutor().execute(() -> {
            Thread.currentThread().setName("PylintRunnerThread");
            PylintResult result = PylintTerminal.this.runner.runPylint();
            // Access UI is prohibited from non-dispatch thread.
            ApplicationManager.getApplication().invokeLater(() -> {
                PylintTerminal.this.setReady(result);
                if ((result == null) || (result.getViolationsCount() == 0)) {
                    return;
                }
                if (result.getRetCode() != 0) {
                    PylintTerminal.this.makeErrorMap(result);
                    PylintTerminal.this.collapsed = new HashSet<>();
                    PylintTerminal.this.renderList();
                    PylintTerminal.this.errorsList.setSelectedIndex(0);
                }
            });
        });
    }

    private void setWaiting() {
        errorsList.setForeground(new JBColor(new Color(GRAY), new Color(DARK_GRAY)));
        errorsList.setCellRenderer(defaultRenderer);
        pylintStatus.setText("Running...");
        pylintStatus.setForeground(new JBColor(new Color(BLACK), new Color(GRAY)));
        pylintStatus.setBackground(new JBColor(new Color(WHITE), new Color(BLACK)));
        errorsList.setListData(new PylintViolation[]{});
        errorsList.setPaintBusy(true);
        pylintRun.setText("Wait...");
        pylintRun.setEnabled(false);
    }

    private void setReady(PylintResult result) {
        pylintRun.setText("Run");
        pylintRun.setEnabled(true);
        errorsList.setPaintBusy(false);
        if (result == null) { // IO exception happened
            pylintStatus.setText("Internal problem...");
            return;
        }
        if (result.getRetCode() == 0) {
            pylintStatus.setText(String.format("PASSED. %s", result.getRating()));
            pylintStatus.setForeground(new JBColor(new Color(BLACK), new Color(100, 255, 100)));
            pylintStatus.setBackground(new JBColor(new Color(LIGHT_GREEN), new Color(BLACK)));
            // clear debug output
            errorsList.setListData(new PylintViolation[]{});
        } else {
            String suffix = result.getViolationsCount() != 1 ? "s" : "";
            pylintStatus.setText(String.format("FAILED: %d violation%s. %s",
                    result.getViolationsCount(), suffix, result.getRating()));
            pylintStatus.setForeground(new JBColor(new Color(BLACK), new Color(255, 100, 100)));
            pylintStatus.setBackground(new JBColor(new Color(LIGHT_RED), new Color(BLACK)));
            if (result.getViolationsCount() == 0) {
                // keep debug output
                return;
            }
        }
        errorsList.setForeground(new JBColor(new Color(BLACK), new Color(GRAY)));
        errorsList.setCellRenderer(pylintRenderer);
    }

    private void makeErrorMap(PylintResult result) {
        HashMap<String, ArrayList<PylintViolation>> map = new HashMap<>();
        ArrayList<PylintViolation> errors = result.getViolations();
        ArrayList<String> files = new ArrayList<>();
        for (PylintViolation next : errors) {
            String file = next.getFile();
            if (!map.containsKey(file)) {
                map.put(file, new ArrayList<>());
            }
            map.get(file).add(next);
            if (!files.contains(file)) {
                files.add(file);
            }
        }
        errorMap = map;
        errorFiles = files;
    }

    public void renderList() {
        ArrayList<PylintViolation> lines = new ArrayList<>();
        for (String file : errorFiles) {
            boolean toggle = collapsed.contains(file);
            int[] violationsCounts = new int[6];
            for (PylintViolation error : errorMap.get(file)) {
                if (error.isViolation()) {
                    violationsCounts[error.getLevel()]++;
                }
            }
            PylintViolation title = new PylintViolation(file, PylintViolation.HEADER, violationsCounts);
            if (toggle) {
                title.toggle();
            }
            lines.add(title);
            if (!collapsed.contains(file)) {
                lines.addAll(errorMap.get(file));
            }
        }
        PylintViolation[] data = new PylintViolation[lines.size()];
        data = lines.toArray(data);
        errorsList.setListData(data);
    }

    private void openError(int index) {
        if ((index >= errorsList.getModel().getSize()) | (index < 0)) {
            return;
        }
        if (runner.isRunning()) {
            return;
        }
        PylintViolation error = errorsList.getModel().getElementAt(index);
        String directory = project.getBaseDir().getPath();
        if (error.isViolation()) {
            String file = error.getFile();
            int lineno = max(error.getLine() - 1, 0);
            int colOffset = max(error.getColumn() - 1, 0);
            VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(directory + File.separator + file);
            // May be null if an error is shown in a file beyond rSERVER
            // (e.g. typeshed or a deleted file because of a bug).
            if (vf != null) {
                FileEditor[] fileEditors = FileEditorManagerEx.getInstanceEx(project).openFile(vf, true);
                if (fileEditors[0] instanceof TextEditor) {
                    Editor editor = ((TextEditor) fileEditors[0]).getEditor();
                    LogicalPosition pos = new LogicalPosition(lineno, colOffset);
                    editor.getCaretModel().getPrimaryCaret().moveToLogicalPosition(pos);
                    editor.getSelectionModel().selectLineAtCaret();
                    editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                }
            }
        }
    }
}
