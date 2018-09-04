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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.leinardi.pycharm.pylint.PylintBundle;
import com.leinardi.pycharm.pylint.PylintConfigService;
import com.leinardi.pycharm.pylint.plapi.PylintRunner;
import com.leinardi.pycharm.pylint.util.Icons;
import com.leinardi.pycharm.pylint.util.Notifications;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Paths;

import static org.apache.commons.lang.StringUtils.isBlank;

public class PylintConfigPanel {
    private JTextField pathToPylintTextField;
    private JTextField pathToPylintrcFileTextField;
    private JPanel rootPanel;
    private JButton browseButton;
    private JButton testButton;
    private JButton browseButtonPylintrc;
    private Project project;

    public PylintConfigPanel(Project project) {
        this.project = project;
        PylintConfigService pylintConfigService = PylintConfigService.getInstance(project);
        browseButton.setAction(new BrowseAction());
        testButton.setAction(new TestAction());
        browseButtonPylintrc.setAction(new BrowsePylintrcAction());
        pathToPylintTextField.setText(pylintConfigService.getPathToPylint());
        pathToPylintrcFileTextField.setText(pylintConfigService.getPathToPylintrcFile());
    }

    public JPanel getPanel() {
        return rootPanel;
    }

    public String getPathToPylint() {
        return pathToPylintTextField.getText();
    }

    public String getPathToPylintrcFile() {
        return pathToPylintrcFileTextField.getText();
    }

    private String fileLocation(String filename) {
        if (new File(filename).exists()) {
            return filename;
        }

        final File projectRelativePath = projectRelativeFileOf(filename);
        if (projectRelativePath.exists()) {
            return projectRelativePath.getAbsolutePath();
        }

        return filename;
    }

    private File projectRelativeFileOf(final String filename) {
        return Paths.get(new File(project.getBasePath(), filename).getAbsolutePath())
                .normalize()
                .toAbsolutePath()
                .toFile();
    }

    private String trim(final String text) {
        if (text != null) {
            return text.trim();
        }
        return null;
    }

    private final class BrowseAction extends AbstractAction {

        BrowseAction() {
            putValue(Action.NAME, PylintBundle.message(
                    "config.file.browse.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    PylintBundle.message("config.file.browse.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    PylintBundle.message("config.file.browse.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final VirtualFile toSelect;
            final String filename = trim(pathToPylintTextField.getText());
            final String configFilePath = fileLocation(filename);
            if (!isBlank(configFilePath)) {
                toSelect = LocalFileSystem.getInstance().findFileByPath(configFilePath);
            } else {
                toSelect = project.getBaseDir();
            }

            final FileChooserDescriptor descriptor = new FileChooserDescriptor(
                    true,
                    false,
                    false,
                    false,
                    false,
                    false);
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, project, toSelect);
            if (chosen != null) {
                final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
                pathToPylintTextField.setText(newConfigFile.getAbsolutePath());
            }
        }
    }

    private final class BrowsePylintrcAction extends AbstractAction {

        BrowsePylintrcAction() {
            putValue(Action.NAME, PylintBundle.message(
                    "config.file.browse.text"));
            putValue(Action.SHORT_DESCRIPTION,
                    PylintBundle.message("config.file.pylintrc.browse.tooltip"));
            putValue(Action.LONG_DESCRIPTION,
                    PylintBundle.message("config.file.pylintrc.browse.tooltip"));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final VirtualFile toSelect;
            final String filename = trim(pathToPylintrcFileTextField.getText());
            final String configFilePath = fileLocation(filename);
            if (!isBlank(configFilePath)) {
                toSelect = LocalFileSystem.getInstance().findFileByPath(configFilePath);
            } else {
                toSelect = project.getBaseDir();
            }

            final FileChooserDescriptor descriptor = new FileChooserDescriptor(
                    true,
                    false,
                    false,
                    false,
                    false,
                    false);
            final VirtualFile chosen = FileChooser.chooseFile(descriptor, project, toSelect);
            if (chosen != null) {
                final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
                pathToPylintrcFileTextField.setText(newConfigFile.getAbsolutePath());
            }
        }
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
