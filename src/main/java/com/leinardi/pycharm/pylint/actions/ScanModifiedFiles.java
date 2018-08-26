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

package com.leinardi.pycharm.pylint.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.util.VfUtil;

import java.util.List;

/**
 * Scan modified files.
 * <p/>
 * If the project is not setup to use VCS then no files will be scanned.
 */
public class ScanModifiedFiles extends BaseAction {

    private static final Logger LOG = Logger.getInstance(ScanModifiedFiles.class);

    @Override
    public final void actionPerformed(final AnActionEvent event) {
        Project project;
        try {
            project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }

            //            final ToolWindow toolWindow = ToolWindowManager.getInstance(project)
            //                    .getToolWindow(PylintToolWindowPanel.ID_TOOLWINDOW);

            final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            project.getComponent(PylintPlugin.class).asyncScanFiles(
                    VfUtil.filterOnlyPythonProjectFiles(project, changeListManager.getAffectedFiles())
                    /*, getSelectedOverride(toolWindow)*/);
        } catch (Throwable e) {
            LOG.warn("Modified files scan failed", e);
        }
    }

    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        Project project = null;
        try {
            project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                return;
            }

            final PylintPlugin pylintPlugin = project.getComponent(PylintPlugin.class);
            if (pylintPlugin == null) {
                throw new IllegalStateException("Couldn't get pylint plugin");
            }

            final Presentation presentation = event.getPresentation();

            // disable if no files are modified
            final List<VirtualFile> modifiedFiles = ChangeListManager.getInstance(project).getAffectedFiles();
            if (modifiedFiles.isEmpty()) {
                presentation.setEnabled(false);
            } else {
                presentation.setEnabled(!pylintPlugin.isScanInProgress());
            }
        } catch (Throwable e) {
            LOG.warn("Button update failed.", e);
        }
    }
}
