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
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.leinardi.pycharm.pylint.toolwindow.PylintToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.leinardi.pycharm.pylint.actions.ToolWindowAccess.actOnToolWindowPanel;
import static com.leinardi.pycharm.pylint.actions.ToolWindowAccess.getFromToolWindowPanel;
import static com.leinardi.pycharm.pylint.actions.ToolWindowAccess.toolWindow;

/**
 * Action to toggle error display in tool window.
 */
public class DisplayInfo extends DumbAwareToggleAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public boolean isSelected(final @NotNull AnActionEvent event) {
        final Project project = getEventProject(event);
        if (project == null) {
            return false;
        }

        Boolean displayingInfo = getFromToolWindowPanel(toolWindow(project),
                PylintToolWindowPanel::isDisplayingInfo);
        return Objects.requireNonNullElse(displayingInfo, false);
    }

    @Override
    public void setSelected(final @NotNull AnActionEvent event, final boolean selected) {
        final Project project = getEventProject(event);
        if (project == null) {
            return;
        }

        actOnToolWindowPanel(toolWindow(project), panel -> {
            panel.setDisplayingInfo(selected);
            panel.filterDisplayedResults();
        });
    }
}
