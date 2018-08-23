package com.leinardi.plugins.pylint_plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import icons.PylintIcons;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PylintConfig extends DialogWrapper {
    private JPanel contentPane;
    private JLabel logo;
    private JTextField command;
    private JTextField path;
    private final PylintConfigService mConfig;

    public PylintConfig(Project project) {
        super(project);
        setModal(true);
        init();
        setTitle("Pylint Plugin Configuration");
        mConfig = PylintConfigService.getInstance(project);
        String storedCmd = mConfig.getExecutableName();
        if (storedCmd != null) {
            this.command.setText(storedCmd);
        } else {
            this.command.setText(PylintToolWindowFactory.DEFAULT_PYLINT_COMMAND);
        }
        String storedPath = mConfig.getPathSuffix();
        if (storedPath != null) {
            this.path.setText(storedPath);
        } else {
            this.path.setText(PylintToolWindowFactory.DEFAULT_PYLINT_PATH_SUFFIX);
        }
        logo.setIcon(PylintIcons.PYLINT_BIG);
        command.setCaretPosition(0);
        path.setCaretPosition(0);
    }

    @Override
    protected void doOKAction() {
        mConfig.setExecutableName(command.getText());
        mConfig.setPathSuffix(path.getText());
        super.doOKAction();
    }

    public JComponent createCenterPanel() {
        return this.contentPane;
    }

    public ValidationInfo doValidate() {
        if (this.command.getText().equals("")) {
            return new ValidationInfo("Command cannot be empty", this.command);
        }
        return null;
    }

}
