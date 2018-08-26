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

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.leinardi.pycharm.pylint.checker.Problem;
import com.leinardi.pycharm.pylint.checker.ScanFiles;
import com.leinardi.pycharm.pylint.checker.ScannerListener;
import com.leinardi.pycharm.pylint.checker.UiFeedbackScannerListener;
import com.leinardi.pycharm.pylint.exception.PylintPluginException;
import com.leinardi.pycharm.pylint.util.Async;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static com.leinardi.pycharm.pylint.util.Async.whenFinished;

/**
 * Main class for the Pylint scanning plug-in.
 */
public final class PylintPlugin implements ProjectComponent {

    /**
     * The plugin ID. Caution: It must be identical to the String set in build.gradle at intellij.pluginName
     */
    public static final String ID_PLUGIN = "Pylint-IDEA";

    public static final String ID_MODULE_PLUGIN = "Pylint-IDEA-Module";

    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(PylintPlugin.class);

    private final Set<Future<?>> checksInProgress = new HashSet<>();
    private final Project project;
    //    private final PluginConfigurationManager configurationManager;

    /**
     * Construct a plug-in instance for the given project.
     *
     * @param project the current project.
     */
    public PylintPlugin(@NotNull final Project project
            /*, @NotNull final PluginConfigurationManager configurationManager*/) {
        this.project = project;
        //        this.configurationManager = configurationManager;

        LOG.info("Pylint Plugin loaded with project base dir: \"" + getProjectPath() + "\"");

        disablePylintLogging();
    }

    private void disablePylintLogging() {
        try {
            // This is a nasty hack to get around IDEA's DialogAppender sending any errors to the Event Log,
            // which would result in Pylint parse errors spamming the Event Log.
            org.apache.log4j.Logger.getLogger("com.puppycrawl.tools.pylint.TreeWalker").setLevel(Level.OFF);
        } catch (Exception e) {
            LOG.warn("Unable to suppress logging from Pylint's TreeWalker", e);
        }
    }

    public Project getProject() {
        return project;
    }

    @Nullable
    private File getProjectPath() {
        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        return new File(baseDir.getPath());
    }

    //    /**
    //     * Get the plugin configuration.
    //     *
    //     * @return the plug-in configuration.
    //     */
    //    public PluginConfigurationManager configurationManager() {
    //        return configurationManager;
    //    }

    /**
     * Is a scan in progress?
     * <p>
     * This is only expected to be called from the event thread.
     *
     * @return true if a scan is in progress.
     */
    public boolean isScanInProgress() {
        synchronized (checksInProgress) {
            return !checksInProgress.isEmpty();
        }
    }

    @Override
    public void projectOpened() {
        LOG.debug("Project opened.");
        //        notifyUserIfPluginUpdated();
    }

    //    private void notifyUserIfPluginUpdated() {
    //        if (!Objects.equals(currentPluginVersion(), lastActivePluginVersion())) {
    //            Notifications.showInfo(project, message("plugin.update", currentPluginVersion()));
    //            configurationManager.setCurrent(PluginConfigurationBuilder.from(configurationManager.getCurrent())
    //                    .withLastActivePluginVersion(currentPluginVersion())
    //                    .build(), false);
    //        }
    //    }

    //    private String lastActivePluginVersion() {
    //        return configurationManager.getCurrent().getLastActivePluginVersion();
    //    }

    public static String currentPluginVersion() {
        final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(ID_PLUGIN));
        if (plugin != null) {
            return plugin.getVersion();
        }
        return "unknown";
    }

    @Override
    public void projectClosed() {
        LOG.debug("Project closed; invalidating checkers.");

        //        invalidateCheckerCache();
    }

    //    private void invalidateCheckerCache() {
    //        ServiceManager.getService(project, CheckerFactoryCache.class).invalidate();
    //    }

    @Override
    @NotNull
    public String getComponentName() {
        return ID_PLUGIN;
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    public static void processErrorAndLog(@NotNull final String action, @NotNull final Throwable e) {
        LOG.warn(action + " failed", e);
    }

    private <T> Future<T> checkInProgress(final Future<T> checkFuture) {
        synchronized (checksInProgress) {
            if (!checkFuture.isDone()) {
                checksInProgress.add(checkFuture);
            }
        }
        return checkFuture;
    }

    public void stopChecks() {
        synchronized (checksInProgress) {
            checksInProgress.forEach(task -> task.cancel(true));
            checksInProgress.clear();
        }
    }

    public <T> void checkComplete(final Future<T> task) {
        if (task == null) {
            return;
        }

        synchronized (checksInProgress) {
            checksInProgress.remove(task);
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void asyncScanFiles(final List<VirtualFile> files/*, final ConfigurationLocation
            overrideConfigLocation*/) {
        LOG.info("Scanning current file(s).");

        if (files == null || files.isEmpty()) {
            LOG.debug("No files provided.");
            return;
        }

        final ScanFiles checkFiles = new ScanFiles(this, files/*, overrideConfigLocation*/);
        checkFiles.addListener(new UiFeedbackScannerListener(this));
        runAsyncCheck(checkFiles);
    }

    public Map<PsiFile, List<Problem>> scanFiles(@NotNull final List<VirtualFile> files) {
        if (files.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return whenFinished(runAsyncCheck(new ScanFiles(this, files/*, null*/))).get();
        } catch (final Throwable e) {
            LOG.warn("ERROR scanning files", e);
            return Collections.emptyMap();
        }
    }

    private Future<Map<PsiFile, List<Problem>>> runAsyncCheck(final ScanFiles checker) {
        final Future<Map<PsiFile, List<Problem>>> checkFilesFuture =
                checkInProgress(Async.executeOnPooledThread(checker));
        checker.addListener(new ScanCompletionTracker(checkFilesFuture));
        return checkFilesFuture;
    }

    //    public ConfigurationLocation getConfigurationLocation(@Nullable final Module module, @Nullable final
    //    ConfigurationLocation override) {
    //        if (override != null) {
    //            return override;
    //        }
    //
    //        if (module != null) {
    //            final PylintModuleConfiguration moduleConfiguration = ModuleServiceManager.getService(module,
    //                    PylintModuleConfiguration.class);
    //            if (moduleConfiguration == null) {
    //                throw new IllegalStateException("Couldn't get pylint module configuration");
    //            }
    //
    //            if (moduleConfiguration.isExcluded()) {
    //                return null;
    //            }
    //            return moduleConfiguration.getActiveConfiguration();
    //        }
    //        return configurationManager().getCurrent().getActiveLocation();
    //    }

    private class ScanCompletionTracker implements ScannerListener {

        private final Future<Map<PsiFile, List<Problem>>> future;

        ScanCompletionTracker(final Future<Map<PsiFile, List<Problem>>> future) {
            this.future = future;
        }

        @Override
        public void scanStarting(final List<PsiFile> filesToScan) {
        }

        @Override
        public void filesScanned(final int count) {
        }

        @Override
        public void scanCompletedSuccessfully(
                final Map<PsiFile, List<Problem>> scanResults) {
            checkComplete(future);
        }

        @Override
        public void scanFailedWithError(final PylintPluginException error) {
            checkComplete(future);
        }
    }
}
