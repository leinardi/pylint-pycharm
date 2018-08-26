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
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.toolwindow.PylintToolWindowPanel;
import com.leinardi.pycharm.pylint.util.VfUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Scan files in the current change-list.
 */
public class ScanCurrentChangeList extends BaseAction {

    private static final Logger LOG = Logger.getInstance(ScanCurrentChangeList.class);

    @Override
    public final void actionPerformed(final AnActionEvent event) {
        Project project = null;
        try {
            project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }

            final ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                    .getToolWindow(PylintToolWindowPanel.ID_TOOLWINDOW);

            final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            project.getComponent(PylintPlugin.class)
                    .asyncScanFiles(VfUtil.filterOnlyPythonProjectFiles(project,
                            filesFor(changeListManager.getDefaultChangeList()))
                            /*, getSelectedOverride(toolWindow)*/);
        } catch (Throwable e) {
            LOG.warn("Modified files scan failed", e);
        }
    }

    private List<VirtualFile> filesFor(final LocalChangeList changeList) {
        if (changeList == null || changeList.getChanges() == null) {
            return Collections.emptyList();
        }

        final Collection<VirtualFile> filesInChanges = new HashSet<>();
        for (Change change : changeList.getChanges()) {
            if (change.getVirtualFile() != null) {
                filesInChanges.add(change.getVirtualFile());
            }
        }

        return new ArrayList<>(filesInChanges);
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

            final LocalChangeList changeList = ChangeListManager.getInstance(project).getDefaultChangeList();
            if (changeList == null || changeList.getChanges() == null || changeList.getChanges().size() == 0) {
                presentation.setEnabled(false);
            } else {
                presentation.setEnabled(!pylintPlugin.isScanInProgress());
            }
        } catch (Throwable e) {
            LOG.warn("Button update failed.", e);
        }
    }
}
