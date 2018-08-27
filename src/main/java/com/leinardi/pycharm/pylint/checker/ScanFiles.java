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

package com.leinardi.pycharm.pylint.checker;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.exception.PylintPluginException;
import com.leinardi.pycharm.pylint.plapi.Issue;
import com.leinardi.pycharm.pylint.plapi.ProcessResultsThread;
import com.leinardi.pycharm.pylint.plapi.PylintRunner;
import com.leinardi.pycharm.pylint.util.Notifications;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static java.util.Collections.emptyMap;

public class ScanFiles implements Callable<Map<PsiFile, List<Problem>>> {

    private static final Logger LOG = Logger.getInstance(ScanFiles.class);

    private final List<PsiFile> files;
    //    private final Map<Module, Set<PsiFile>> moduleToFiles;
    private final Set<ScannerListener> listeners = new HashSet<>();
    private final PylintPlugin plugin;
    private final int tabWidth = 4;

    public ScanFiles(@NotNull final PylintPlugin pylintPlugin,
                     @NotNull final List<VirtualFile> virtualFiles/*,
                     @Nullable final ConfigurationLocation overrideConfigLocation*/) {
        this.plugin = pylintPlugin;
        //        this.overrideConfigLocation = overrideConfigLocation;

        files = findAllFilesFor(virtualFiles);
        //        moduleToFiles = mapsModulesToFiles();
    }

    private List<PsiFile> findAllFilesFor(@NotNull final List<VirtualFile> virtualFiles) {
        final List<PsiFile> childFiles = new ArrayList<>();
        final PsiManager psiManager = PsiManager.getInstance(this.plugin.getProject());
        for (final VirtualFile virtualFile : virtualFiles) {
            childFiles.addAll(buildFilesList(psiManager, virtualFile));
        }
        return childFiles;
    }

    //    private Map<Module, Set<PsiFile>> mapsModulesToFiles() {
    //        final Map<Module, Set<PsiFile>> modulesToFiles = new HashMap<>();
    //        for (final PsiFile file : files) {
    //            final Module module = ModuleUtil.findModuleForPsiElement(file);
    //            Set<PsiFile> filesForModule = modulesToFiles.computeIfAbsent(module, key -> new HashSet<>());
    //            filesForModule.add(file);
    //        }
    //        return modulesToFiles;
    //    }

    @Override
    public final Map<PsiFile, List<Problem>> call() {
        try {
            fireCheckStarting(files);
            //            final Pair<ConfigurationLocationResult, Map<PsiFile, List<Problem>>> scanResult =
            //                    processFilesForModuleInfoAndScan();
            return scanCompletedSuccessfully(checkFiles(new HashSet<>(files)));
        } catch (final InterruptedIOException e) {
            LOG.debug("Scan cancelled by PyCharm", e);
            return scanCompletedSuccessfully(/*resultOf(PRESENT),*/ emptyMap());
        } catch (final PylintPluginException e) {
            LOG.warn("An error occurred while scanning a file.", e);
            return scanFailedWithError(e);
        } catch (final Throwable e) {
            LOG.warn("An error occurred while scanning a file.", e);
            return scanFailedWithError(new PylintPluginException("An error occurred while scanning a file.", e));
        }
    }

    private Map<String, PsiFile> mapFilesToElements(final List<ScannableFile> filesToScan) {
        final Map<String, PsiFile> filePathsToElements = new HashMap<>();
        for (ScannableFile scannableFile : filesToScan) {
            filePathsToElements.put(scannableFile.getAbsolutePath(), scannableFile.getPsiFile());
        }
        return filePathsToElements;
    }

    private String withTrailingSeparator(final String path) {
        if (path != null && !path.endsWith(File.separator)) {
            return path + File.separator;
        }
        return path;
    }

    private Map<PsiFile, List<Problem>> checkFiles(final Set<PsiFile> filesToScan) throws InterruptedIOException {
        final List<ScannableFile> scannableFiles = new ArrayList<>();
        try {
            scannableFiles.addAll(ScannableFile.createAndValidate(filesToScan, plugin));

            return scan(scannableFiles);
        } finally {
            scannableFiles.forEach(ScannableFile::deleteIfRequired);
        }
    }

    private Map<PsiFile, List<Problem>> scan(final List<ScannableFile> filesToScan) throws InterruptedIOException {
        Map<String, PsiFile> fileNamesToPsiFiles = mapFilesToElements(filesToScan);
        List<Issue> errors = PylintRunner.scan(plugin.getProject(), fileNamesToPsiFiles.keySet());
        String baseDir = plugin.getProject().getBasePath();
        final ProcessResultsThread findThread = new ProcessResultsThread(false, tabWidth, baseDir,
                errors, fileNamesToPsiFiles);

        final Application application = ApplicationManager.getApplication();
        //        if (application != null) {  // can be null in unit tests
        // TODO Make sure that this does not block ... it seems that we are not starting a new thread.
        //      Sometimes, the editor is non-responsive because Checkstyle is still processing results.
        //      Problem: OpScan currently expects the ready-made list of problems synchronously.
        if (application.isDispatchThread()) {
            findThread.run();
        } else {
            application.runReadAction(findThread);
        }
        return findThread.getProblems();
        //        }
    }

    private Map<PsiFile, List<Problem>> scanFailedWithError(final PylintPluginException e) {
        Notifications.showException(plugin.getProject(), e);
        fireScanFailedWithError(e);

        return emptyMap();
    }

    private Map<PsiFile, List<Problem>> scanCompletedSuccessfully(/*final ConfigurationLocationResult
    configurationLocationResult,*/
            final Map<PsiFile, List<Problem>> filesToProblems) {
        fireScanCompletedSuccessfully(/*configurationLocationResult, */filesToProblems);
        return filesToProblems;
    }

    public void addListener(final ScannerListener listener) {
        listeners.add(listener);
    }

    private void fireCheckStarting(final List<PsiFile> filesToScan) {
        listeners.forEach(listener -> listener.scanStarting(filesToScan));
    }

    private void fireScanCompletedSuccessfully(/*final ConfigurationLocationResult configLocationResult,*/
            final Map<PsiFile, List<Problem>> fileResults) {
        listeners.forEach(listener -> listener.scanCompletedSuccessfully(/*configLocationResult, */fileResults));
    }

    private void fireScanFailedWithError(final PylintPluginException error) {
        listeners.forEach(listener -> listener.scanFailedWithError(error));
    }

    private void fireFilesScanned(final int count) {
        listeners.forEach(listener -> listener.filesScanned(count));
    }

    private List<PsiFile> buildFilesList(final PsiManager psiManager, final VirtualFile virtualFile) {
        final List<PsiFile> allChildFiles = new ArrayList<>();
        ApplicationManager.getApplication().runReadAction(() -> {
            final FindChildFiles visitor = new FindChildFiles(virtualFile, psiManager);
            VfsUtilCore.visitChildrenRecursively(virtualFile, visitor);
            allChildFiles.addAll(visitor.locatedFiles);
        });
        return allChildFiles;
    }

    //    private Pair<ConfigurationLocationResult, Map<PsiFile, List<Problem>>> processFilesForModuleInfoAndScan() {
    //        final Map<PsiFile, List<Problem>> fileResults = new HashMap<>();
    //
    //        for (final Module module : moduleToFiles.keySet()) {
    //            if (module == null) {
    //                continue;
    //            }
    //
    //            final ConfigurationLocationResult locationResult = configurationLocation(overrideConfigLocation,
    // module);
    //            if (locationResult.status != PRESENT) {
    //                return pair(locationResult, emptyMap());
    //            }
    //
    //            final Set<PsiFile> filesForModule = moduleToFiles.get(module);
    //            if (filesForModule.isEmpty()) {
    //                continue;
    //            }
    //
    //            fileResults.putAll(filesWithProblems(filesForModule,
    //                    checkFiles(module, filesForModule, locationResult.location)));
    //            fireFilesScanned(filesForModule.size());
    //        }
    //
    //        return pair(resultOf(PRESENT), fileResults);
    //    }

    //    @NotNull
    //    private Map<PsiFile, List<Problem>> filesWithProblems(final Set<PsiFile> filesForModule,
    //                                                          final Map<PsiFile, List<Problem>> moduleFileResults) {
    //        final Map<PsiFile, List<Problem>> moduleResults = new HashMap<>();
    //        for (final PsiFile psiFile : filesForModule) {
    //            final List<Problem> resultsForFile = moduleFileResults.get(psiFile);
    //            if (resultsForFile != null && !resultsForFile.isEmpty()) {
    //                moduleResults.put(psiFile, new ArrayList<>(resultsForFile));
    //            }
    //        }
    //        return moduleResults;
    //    }

    //    @NotNull
    //    private ConfigurationLocationResult configurationLocation(final ConfigurationLocation override,
    //                                                              final Module module) {
    //        final ConfigurationLocation location = plugin.getConfigurationLocation(module, override);
    //        if (location == null) {
    //            return resultOf(NOT_PRESENT);
    //        }
    //        if (location.isBlacklisted()) {
    //            return resultOf(location, BLACKLISTED);
    //        }
    //        return resultOf(location, PRESENT);
    //    }
    //
    //    private Map<PsiFile, List<Problem>> checkFiles(final Module module,
    //                                                   final Set<PsiFile> filesToScan,
    //                                                   final ConfigurationLocation configurationLocation) {
    //        final List<ScannableFile> scannableFiles = new ArrayList<>();
    //        try {
    //            scannableFiles.addAll(ScannableFile.createAndValidate(filesToScan, plugin, module));
    //
    //            return checkerFactory(module.getProject()).checker(module, configurationLocation)
    //                    .map(checker -> checker.scan(scannableFiles,
    //                            plugin.configurationManager().getCurrent().isSuppressErrors()))
    //                    .orElseThrow(() -> new PylintPluginException("Could not create checker"));
    //        } finally {
    //            scannableFiles.forEach(ScannableFile::deleteIfRequired);
    //        }
    //    }

    //    private CheckerFactory checkerFactory(final Project project) {
    //        return ServiceManager.getService(project, CheckerFactory.class);
    //    }

    private static class FindChildFiles extends VirtualFileVisitor {

        private final VirtualFile virtualFile;
        private final PsiManager psiManager;

        final List<PsiFile> locatedFiles = new ArrayList<>();

        FindChildFiles(final VirtualFile virtualFile, final PsiManager psiManager) {
            this.virtualFile = virtualFile;
            this.psiManager = psiManager;
        }

        @Override
        public boolean visitFile(@NotNull final VirtualFile file) {
            if (!file.isDirectory()) {
                final PsiFile psiFile = psiManager.findFile(virtualFile);
                if (psiFile != null) {
                    locatedFiles.add(psiFile);
                }
            }
            return true;
        }
    }

}
