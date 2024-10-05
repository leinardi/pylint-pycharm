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
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.wm.ToolWindow;
import com.leinardi.pycharm.pylint.PylintBundle;
import com.leinardi.pycharm.pylint.PylintPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.leinardi.pycharm.pylint.actions.ToolWindowAccess.actOnToolWindowPanel;
import static java.util.Optional.ofNullable;

/**
 * Base class for plug-in actions.
 */
public abstract class BaseAction extends DumbAwareAction {

    private static final Logger LOG = Logger.getInstance(BaseAction.class);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(final @NotNull AnActionEvent event) {
        try {
            final Project project = getEventProject(event);
            final Presentation presentation = event.getPresentation();

            // check a project is loaded
            if (project == null) {
                presentation.setEnabled(false);
                presentation.setVisible(false);

                return;
            }

            final PylintPlugin pylintPlugin = project.getService(PylintPlugin.class);
            if (pylintPlugin == null) {
                throw new IllegalStateException("Couldn't get pylint plugin");
            }

            // check if tool window is registered
            final ToolWindow toolWindow = ToolWindowAccess.toolWindow(project);
            if (toolWindow == null) {
                presentation.setEnabled(false);
                presentation.setVisible(false);

                return;
            }

            // enable
            presentation.setEnabled(toolWindow.isAvailable());
            presentation.setVisible(true);
        } catch (Throwable e) {
            LOG.warn("Action update failed", e);
        }
    }

    protected void setProgressText(final ToolWindow toolWindow, final String progressTextKey) {
        actOnToolWindowPanel(toolWindow, panel -> panel.setProgressText(PylintBundle.message(progressTextKey)));
    }

    protected Optional<Project> project(@NotNull final AnActionEvent event) {
        return ofNullable(getEventProject(event));
    }

    protected boolean containsAtLeastOneFile(@NotNull final VirtualFile... files) {
        final var result = new AtomicBoolean(false);
        for (VirtualFile file : files) {
            VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<>() {
                @Override
                public @NotNull Result visitFileEx(@NotNull final VirtualFile file) {
                    if (!file.isDirectory() && file.isValid()) {
                        result.set(true);
                        return SKIP_CHILDREN;
                    }
                    return CONTINUE;
                }
            });

            if (result.get()) {
                break;
            }
        }
        return result.get();
    }
}
