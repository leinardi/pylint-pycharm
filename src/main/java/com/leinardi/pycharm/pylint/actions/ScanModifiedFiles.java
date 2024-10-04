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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.util.VfUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Scan modified files.
 * <p/>
 * If the project is not setup to use VCS then no files will be scanned.
 */
public class ScanModifiedFiles extends BaseAction {

    private static final Logger LOG = Logger.getInstance(ScanModifiedFiles.class);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public final void actionPerformed(final @NotNull AnActionEvent event) {
        project(event).ifPresent(project -> {
            try {
                final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
                project.getService(PylintPlugin.class).asyncScanFiles(
                        VfUtil.filterOnlyPythonProjectFiles(project, changeListManager.getAffectedFiles())
                );
            } catch (Throwable e) {
                LOG.warn("Modified files scan failed", e);
            }
        });
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();

        project(event).ifPresentOrElse(project -> {
            try {
                final PylintPlugin mypyPlugin = project.getService(PylintPlugin.class);
                if (mypyPlugin == null) {
                    throw new IllegalStateException("Couldn't get mypy plugin");
                }
                // disable if no files are modified
                final List<VirtualFile> modifiedFiles = ChangeListManager.getInstance(project).getAffectedFiles();
                if (modifiedFiles.isEmpty()) {
                    presentation.setEnabled(false);
                } else {
                    presentation.setEnabled(!mypyPlugin.isScanInProgress());
                }
            } catch (Throwable e) {
                LOG.warn("Button update failed.", e);
            }
        }, () -> presentation.setEnabled(false));
    }
}
