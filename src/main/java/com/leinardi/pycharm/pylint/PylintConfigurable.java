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

    private final Project project;

    private final PylintConfigPanel configPanel;
    private final PylintConfigService pylintConfigService;
    //    private final PylintProjectService pylintProjectService;
    //    private final PluginConfigurationManager pluginConfigurationManager;

    public PylintConfigurable(@NotNull final Project project/*,
                              @NotNull final PylintProjectService pylintProjectService,
                              @NotNull final PluginConfigurationManager pluginConfigurationManager*/) {
        this(project, new PylintConfigPanel(project/*, pylintProjectService*/)/*,
                pylintProjectService, pluginConfigurationManager*/);
    }

    PylintConfigurable(@NotNull final Project project,
                       @NotNull final PylintConfigPanel configPanel/*,
                       @NotNull final PylintProjectService pylintProjectService,
                       @NotNull final PluginConfigurationManager pluginConfigurationManager*/) {
        this.project = project;
        this.configPanel = configPanel;
        pylintConfigService = PylintConfigService.getInstance(project);
        //        this.pylintProjectService = pylintProjectService;
        //        this.pluginConfigurationManager = pluginConfigurationManager;
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
        //        final PluginConfiguration oldConfig = pluginConfigurationManager.getCurrent();
        //        final PluginConfiguration newConfig = PluginConfigurationBuilder
        //                .from(configPanel.getPluginConfiguration())
        //                .withScanBeforeCheckin(oldConfig.isScanBeforeCheckin())
        //                .build();
        //
        boolean result = !configPanel.getPathToPylint().equals(pylintConfigService.getPathToPylint());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Has config changed? " + result);
        }
        return result;
    }

    @Override
    public void apply() {
        //        final PluginConfiguration newConfig = PluginConfigurationBuilder.from(configPanel
        // .getPluginConfiguration())
        //                .withScanBeforeCheckin(pluginConfigurationManager.getCurrent().isScanBeforeCheckin())
        //                .build();
        //        pluginConfigurationManager.setCurrent(newConfig, true);
        //
        //        activateCurrentPylintVersion(newConfig.getPylintVersion(), newConfig.getThirdPartyClasspath());
        //        if (!newConfig.isCopyLibs()) {
        //            new TempDirProvider().deleteCopiedLibrariesDir(project);
        //        }
        pylintConfigService.setPathToPylint(configPanel.getPathToPylint());
    }

    //    private void activateCurrentPylintVersion(final String pylintVersion,
    //                                                  final List<String> thirdPartyClasspath) {
    //        // Invalidate cache *before* activating the new Pylint version
    //        getCheckerFactoryCache().invalidate();
    //
    //        pylintProjectService.activatePylintVersion(pylintVersion, thirdPartyClasspath);
    //    }
    //
    //    private CheckerFactoryCache getCheckerFactoryCache() {
    //        return ServiceManager.getService(project, CheckerFactoryCache.class);
    //    }
    //
    //    public void reset() {
    //        final PluginConfiguration pluginConfig = pluginConfigurationManager.getCurrent();
    //        configPanel.showPluginConfiguration(pluginConfig);
    //
    //        activateCurrentPylintVersion(pluginConfig.getPylintVersion(), pluginConfig.getThirdPartyClasspath());
    //    }
    @Override
    public void disposeUIResources() {
        // do nothing
    }
}
