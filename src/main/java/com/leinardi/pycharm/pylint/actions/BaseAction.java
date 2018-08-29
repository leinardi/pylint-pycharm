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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.leinardi.pycharm.pylint.PylintBundle;
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.toolwindow.PylintToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.Optional;

import static com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT;
import static java.util.Optional.ofNullable;

/**
 * Base class for plug-in actions.
 */
public abstract class BaseAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(BaseAction.class);

    @Override
    public void update(final AnActionEvent event) {
        Project project;
        try {
            project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            final Presentation presentation = event.getPresentation();

            // check a project is loaded
            if (project == null) {
                presentation.setEnabled(false);
                presentation.setVisible(false);

                return;
            }

            final PylintPlugin pylintPlugin = project.getComponent(PylintPlugin.class);
            if (pylintPlugin == null) {
                throw new IllegalStateException("Couldn't get pylint plugin");
            }

            // check if tool window is registered
            final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(
                    PylintToolWindowPanel.ID_TOOLWINDOW);
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
        final Content content = toolWindow.getContentManager().getContent(0);
        if (content != null) {
            final JComponent component = content.getComponent();
            // the content instance will be a JLabel while the component initialises
            if (component instanceof PylintToolWindowPanel) {
                final PylintToolWindowPanel panel = (PylintToolWindowPanel) component;
                panel.setProgressText(PylintBundle.message(progressTextKey));
            }
        }
    }

    protected Optional<Project> project(@NotNull final AnActionEvent event) {
        return ofNullable(PROJECT.getData(event.getDataContext()));
    }

    boolean containsAtLeastOneFile(@NotNull final VirtualFile... files) {
        boolean result = false;
        for (VirtualFile file : files) {
            if ((file.isDirectory() && containsAtLeastOneFile(file.getChildren())) || (!file.isDirectory() && file
                    .isValid())) {
                result = true;
                break;
            }
        }
        return result;
    }
}
