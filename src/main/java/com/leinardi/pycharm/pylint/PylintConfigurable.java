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

package com.leinardi.pycharm.pylint;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.leinardi.pycharm.pylint.ui.PylintConfigPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * The "configurable component" required by PyCharm to provide a Swing form for inclusion into the 'Settings'
 * dialog. Registered in {@code plugin.xml} as a {@code projectConfigurable} extension.
 */
public class PylintConfigurable implements Configurable {
    private static final Logger LOG = Logger.getInstance(PylintConfigurable.class);

    private final PylintConfigPanel configPanel;
    private final PylintConfigService pylintConfigService;

    public PylintConfigurable(@NotNull final Project project) {
        this(project, new PylintConfigPanel(project));
    }

    PylintConfigurable(@NotNull final Project project,
                       @NotNull final PylintConfigPanel configPanel) {
        this.configPanel = configPanel;
        pylintConfigService = PylintConfigService.getInstance(project);
    }

    @Override
    public String getDisplayName() {
        return PylintBundle.message("plugin.configuration-name");
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        reset();
        return configPanel.getPanel();
    }

    @Override
    public boolean isModified() {
        boolean result = !configPanel.getPathToPylint().equals(pylintConfigService.getPathToPylint());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Has config changed? " + result);
        }
        return result;
    }

    @Override
    public void apply() {
        pylintConfigService.setPathToPylint(configPanel.getPathToPylint());
    }

    @Override
    public void disposeUIResources() {
        // do nothing
    }
}
