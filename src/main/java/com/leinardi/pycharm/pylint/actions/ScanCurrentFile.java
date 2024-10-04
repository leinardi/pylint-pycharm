/*
 * Copyright 2023 Roberto Leinardi.
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

package com.leinardi.pycharm.pylint.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.util.FileTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import static com.leinardi.pycharm.pylint.actions.ToolWindowAccess.toolWindow;

/**
 * Action to execute a Pylint scan on the current editor file.
 */
public class ScanCurrentFile extends BaseAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ToolWindow toolWindow = toolWindow(project);
                toolWindow.activate(() -> {
                    try {
                        setProgressText(toolWindow, "plugin.status.in-progress.current");

                        final VirtualFile selectedFile = getSelectedFile(project);
                        if (selectedFile != null) {
                            project.getService(PylintPlugin.class).asyncScanFiles(
                                    Collections.singletonList(selectedFile));
                        }

                    } catch (Throwable e) {
                        PylintPlugin.processErrorAndLog("Current File scan", e);
                    }
                });

            } catch (Throwable e) {
                PylintPlugin.processErrorAndLog("Current File scan", e);
            }
        });
    }

    private VirtualFile getSelectedFile(final @NotNull Project project) {
        VirtualFile selectedFile = null;

        final Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedTextEditor != null) {
            selectedFile = FileDocumentManager.getInstance().getFile(selectedTextEditor.getDocument());
        }

        if (selectedFile == null) {
            // this is the preferred solution, but it doesn't respect the focus of split editors at present
            final VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selectedFiles.length > 0) {
                selectedFile = selectedFiles[0];
            }
        }

        // validate selected file against scan scope
        if (selectedFile != null) {
            if (!FileTypes.isPython(selectedFile.getFileType())) {
                selectedFile = null;
            }
        }

        return selectedFile;
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();

        project(event).ifPresentOrElse(project -> {
            try {
                final PylintPlugin mypyPlugin
                        = project.getService(PylintPlugin.class);
                if (mypyPlugin == null) {
                    throw new IllegalStateException("Couldn't get mypy plugin");
                }
                final VirtualFile selectedFile = getSelectedFile(project);

                // disable if no file is selected or scan in progress
                if (selectedFile != null) {
                    presentation.setEnabled(!mypyPlugin.isScanInProgress());
                } else {
                    presentation.setEnabled(false);
                }
            } catch (Throwable e) {
                PylintPlugin.processErrorAndLog("Current File button update", e);
            }
        }, () -> presentation.setEnabled(false));
    }
}
