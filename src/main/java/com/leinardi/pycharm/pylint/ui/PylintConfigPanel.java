/*
 * Copyright 2021 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.pycharm.pylint.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import com.leinardi.pycharm.pylint.PylintBundle;
import com.leinardi.pycharm.pylint.PylintConfigService;
import com.leinardi.pycharm.pylint.plapi.PylintRunner;
import com.leinardi.pycharm.pylint.util.Icons;
import com.leinardi.pycharm.pylint.util.Notifications;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;

public class PylintConfigPanel {
    private JPanel rootPanel;
    private JButton testButton;
    private com.intellij.openapi.ui.TextFieldWithBrowseButton pylintPathField;
    private com.intellij.openapi.ui.TextFieldWithBrowseButton pylintrcPathField;
    private JBTextField argumentsField;
    private Project project;

    public PylintConfigPanel(Project project) {
        this.project = project;
        PylintConfigService pylintConfigService = PylintConfigService.getInstance(project);
        if (pylintConfigService == null) {
            throw new IllegalStateException("PylintConfigService is null");
        }
        testButton.setAction(new TestAction());
        pylintPathField.setText(pylintConfigService.getCustomPylintPath());
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(
                true, false, false, false, false, false);
        pylintPathField.addBrowseFolderListener(
                "",
                PylintBundle.message("config.pylint.path.tooltip"),
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        pylintrcPathField.setText(pylintConfigService.getPylintrcPath());
        pylintrcPathField.addBrowseFolderListener(
                "",
                PylintBundle.message("config.pylintrc.path.tooltip"),
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        argumentsField.setText(pylintConfigService.getPylintArguments());
        argumentsField.getEmptyText().setText(PylintBundle.message("config.optional"));
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public String getPylintPath() {
        return getPylintPath(false);
    }

    public String getPylintPath(boolean autodetect) {
        String path = pylintPathField.getText();
        if (path.isEmpty() && autodetect) {
            return PylintRunner.getPylintPath(project, false);
        }
        return path;
    }

    public String getPylintrcPath() {
        return pylintrcPathField.getText();
    }

    public String getPylintArguments() {
        return argumentsField.getText();
    }

    private void createUIComponents() {
        JBTextField autodetectTextField = new JBTextField();
        autodetectTextField.getEmptyText()
                .setText(PylintBundle.message("config.auto-detect", PylintRunner.getPylintPath(project, false)));
        pylintPathField = new TextFieldWithBrowseButton(autodetectTextField);
        JBTextField optionalTextField = new JBTextField();
        optionalTextField.getEmptyText().setText(PylintBundle.message("config.optional"));
        pylintrcPathField = new TextFieldWithBrowseButton(optionalTextField);
    }

    private final class TestAction extends AbstractAction {

        TestAction() {
            putValue(Action.NAME, PylintBundle.message(
                    "config.pylint.path.test"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            String pathToPylint = getPylintPath(true);
            if (PylintRunner.isPylintPathValid(pathToPylint, project)) {
                testButton.setIcon(Icons.icon("/general/inspectionsOK.png"));
                Notifications.showInfo(
                        project,
                        PylintBundle.message("config.pylint.path.success.message")
                );
            } else {
                testButton.setIcon(Icons.icon("/general/error.png"));
                Notifications.showError(
                        project,
                        PylintBundle.message("config.pylint.path.failure.message", pathToPylint)
                );
            }
        }
    }
}
