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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.toolwindow.PylintToolWindowPanel;

/**
 * Action to execute a Pylint scan on the current project.
 */
public class ScanProject extends BaseAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        try {
            final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) {
                return;
            }

            final PylintPlugin pylintPlugin = project.getComponent(PylintPlugin.class);
            if (pylintPlugin == null) {
                throw new IllegalStateException("Couldn't get pylint plugin");
            }
            //            final ScanScope scope = pylintPlugin.configurationManager().getCurrent().getScanScope();

            final ToolWindow toolWindow = ToolWindowManager.getInstance(
                    project).getToolWindow(PylintToolWindowPanel.ID_TOOLWINDOW);
            toolWindow.activate(() -> {
                try {
                    setProgressText(toolWindow, "plugin.status.in-progress.project");
                    Runnable scanAction = null;
                    //                                        if (scope == ScanScope.Everything) {
                    scanAction = new ScanEverythingAction(project/*, getSelectedOverride(toolWindow)*/);
                    //                    } else {
                    //                    final ProjectRootManager projectRootManager = ProjectRootManager
                    // .getInstance(project);
                    //                    final VirtualFile[] sourceRoots =
                    // projectRootManager.getContentSourceRoots();
                    //                    if (sourceRoots.length > 0) {
                    //                        scanAction = new ScanSourceRootsAction(project, sourceRoots/*,
                    //                                    getSelectedOverride(toolWindow)*/);
                    //                    }
                    //                    }
                    //                    if (scanAction != null) {
                    ApplicationManager.getApplication().runReadAction(scanAction);
                    //                    }
                } catch (Throwable e) {
                    PylintPlugin.processErrorAndLog("Project scan", e);
                }
            });

        } catch (Throwable e) {
            PylintPlugin.processErrorAndLog("Project scan", e);
        }
    }

    @Override
    public void update(final AnActionEvent event) {
        super.update(event);

        try {
            final Presentation presentation = event.getPresentation();

            final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            if (project == null) { // check if we're loading...
                presentation.setEnabled(false);
                return;
            }

            final PylintPlugin pylintPlugin = project.getComponent(PylintPlugin.class);
            if (pylintPlugin == null) {
                throw new IllegalStateException("Couldn't get pylint plugin");
            }
            //            final ScanScope scope = pylintPlugin.configurationManager().getCurrent().getScanScope();

            VirtualFile[] sourceRoots = null;
            //            if (scope == ScanScope.Everything) {
            sourceRoots = new VirtualFile[]{project.getBaseDir()};
            //            } else {
            //            final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
            //            sourceRoots = projectRootManager.getContentSourceRoots();
            //            }

            // disable if no files are selected or scan in progress
            if (containsAtLeastOneFile(sourceRoots)) {
                presentation.setEnabled(!pylintPlugin.isScanInProgress());
            } else {
                presentation.setEnabled(false);
            }
        } catch (Throwable e) {
            PylintPlugin.processErrorAndLog("Project button update", e);
        }
    }

}
