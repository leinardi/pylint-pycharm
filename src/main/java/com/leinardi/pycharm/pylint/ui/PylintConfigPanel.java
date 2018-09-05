/*
 * Copyright 2018 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
    private com.intellij.openapi.ui.TextFieldWithBrowseButton pathToPylintField;
    private com.intellij.openapi.ui.TextFieldWithBrowseButton pathToPylintrcFileField;
    private JPanel rootPanel;
    private JButton testButton;
    private Project project;

    public PylintConfigPanel(Project project) {
        this.project = project;
        PylintConfigService pylintConfigService = PylintConfigService.getInstance(project);
        testButton.setAction(new TestAction());
        pathToPylintField.setText(pylintConfigService.getPathToPylint());
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(
                true, false, false, false, false, false);
        pathToPylintField.addBrowseFolderListener(
                "",
                PylintBundle.message("config.file.browse.tooltip"),
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        pathToPylintrcFileField.setText(pylintConfigService.getPathToPylintrcFile());
        pathToPylintrcFileField.addBrowseFolderListener(
                "",
                PylintBundle.message("config.file.pylintrc.browse.tooltip"),
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public String getPathToPylint() {
        return pathToPylintField.getText();
    }

    public String getPathToPylintrcFile() {
        return pathToPylintrcFileField.getText();
    }

    private void createUIComponents() {
        JBTextField optionalTextField = new JBTextField();
        optionalTextField.getEmptyText().setText(PylintBundle.message("config.optional"));
        pathToPylintrcFileField = new TextFieldWithBrowseButton(optionalTextField);
    }

    private final class TestAction extends AbstractAction {

        TestAction() {
            putValue(Action.NAME, PylintBundle.message(
                    "config.pylint.path.test"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            String pathToPylint = getPathToPylint();
            if (PylintRunner.isPathToPylintValid(pathToPylint, project)) {
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
