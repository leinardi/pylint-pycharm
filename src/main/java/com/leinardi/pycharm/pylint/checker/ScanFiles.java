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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.exception.PylintPluginException;
import com.leinardi.pycharm.pylint.plapi.Issue;
import com.leinardi.pycharm.pylint.plapi.PylintRunner;
import com.leinardi.pycharm.pylint.util.Notifications;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
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
    private final Map<Module, Set<PsiFile>> moduleToFiles;
    private final Set<ScannerListener> listeners = new HashSet<>();
    private final PylintPlugin plugin;
    private final String baseDir;
    private final int tabWidth = 4;

    private final Map<PsiFile, List<Problem>> problems = new HashMap<>();

    public ScanFiles(@NotNull final PylintPlugin pylintPlugin,
                     @NotNull final List<VirtualFile> virtualFiles/*,
                     @Nullable final ConfigurationLocation overrideConfigLocation*/) {
        this.plugin = pylintPlugin;
        //        this.overrideConfigLocation = overrideConfigLocation;

        files = findAllFilesFor(virtualFiles);
        moduleToFiles = mapsModulesToFiles();
        baseDir = plugin.getProject().getBasePath();
    }

    private List<PsiFile> findAllFilesFor(@NotNull final List<VirtualFile> virtualFiles) {
        final List<PsiFile> childFiles = new ArrayList<>();
        final PsiManager psiManager = PsiManager.getInstance(this.plugin.getProject());
        for (final VirtualFile virtualFile : virtualFiles) {
            childFiles.addAll(buildFilesList(psiManager, virtualFile));
        }
        return childFiles;
    }

    private Map<Module, Set<PsiFile>> mapsModulesToFiles() {
        final Map<Module, Set<PsiFile>> modulesToFiles = new HashMap<>();
        for (final PsiFile file : files) {
            final Module module = ModuleUtil.findModuleForPsiElement(file);
            Set<PsiFile> filesForModule = modulesToFiles.computeIfAbsent(module, key -> new HashSet<>());
            filesForModule.add(file);
        }
        return modulesToFiles;
    }

    @Override
    public final Map<PsiFile, List<Problem>> call() {
        try {
            fireCheckStarting(files);
            //            final Pair<ConfigurationLocationResult, Map<PsiFile, List<Problem>>> scanResult =
            //                    processFilesForModuleInfoAndScan();
            foo();
            return scanCompletedSuccessfully(/*scanResult.first, scanResult.second*/ problems);
        } catch (final InterruptedIOException e) {
            LOG.debug("Scan cancelled by IDEA", e);
            return scanCompletedSuccessfully(/*resultOf(PRESENT),*/ emptyMap());
        } catch (final PylintPluginException e) {
            LOG.warn("An error occurred while scanning a file.", e);
            return scanFailedWithError(e);
        } catch (final Throwable e) {
            LOG.warn("An error occurred while scanning a file.", e);
            return scanFailedWithError(new PylintPluginException("An error occurred while scanning a file.", e));
        }
    }

    private Map<String, PsiFile> mapFilesToElements(final List<PsiFile> filesToScan) {
        final Map<String, PsiFile> filePathsToElements = new HashMap<>();
        for (PsiFile file : filesToScan) {
            filePathsToElements.put(file.getVirtualFile().getPath(), file);
        }
        return filePathsToElements;
    }

    private String withTrailingSeparator(final String path) {
        if (path != null && !path.endsWith(File.separator)) {
            return path + File.separator;
        }
        return path;
    }

    private String filenameFrom(final Issue issue) {
        return withTrailingSeparator(baseDir) + issue.getPath();
    }

    private void foo() throws InterruptedIOException {
        Map<String, PsiFile> fileNamesToPsiFiles = mapFilesToElements(files);
        PylintRunner pylintRunner = new PylintRunner(plugin, fileNamesToPsiFiles.keySet());
        List<Issue> errors = pylintRunner.scan();

        final Map<PsiFile, List<Integer>> lineLengthCachesByFile = new HashMap<>();

        for (final Issue event : errors) {
            final PsiFile psiFile = fileNamesToPsiFiles.get(filenameFrom(event));
            if (psiFile == null) {
                LOG.info("Could not find mapping for file: " + event.getPath() + " in " + fileNamesToPsiFiles);
                continue;
            }

            List<Integer> lineLengthCache = lineLengthCachesByFile.get(psiFile);
            if (lineLengthCache == null) {
                // we cache the offset of each line as it is created, so as to
                // avoid retreating ground we've already covered.
                lineLengthCache = new ArrayList<>();
                lineLengthCache.add(0); // line 1 is offset 0

                lineLengthCachesByFile.put(psiFile, lineLengthCache);
            }

            //            final Position position = findPosition(lineLengthCache, event, psiFile.textToCharArray());
            //            final PsiElement victim = position.element(psiFile);

            //            if (victim != null) {
            //                addProblemTo(victim, psiFile, event, position.afterEndOfLine);
            //            } else {
            addProblemTo(psiFile, psiFile, event, false);
            //                LOG.debug("Couldn't find victim for error: " + event.getPath() + "(" + event.getLine()
            // + ":"
            //                        + event.getColumn() + ") " + event.getMessage());
            //            }
        }

    }

    private void addProblemTo(final PsiElement victim,
                              final PsiFile psiFile,
                              @NotNull final Issue event,
                              final boolean afterEndOfLine) {
        try {
            addProblem(psiFile,
                    new Problem(
                            victim,
                            event.getMessage(),
                            event.getSeverityLevel(),
                            event.getLine(),
                            event.getColumn(),
                            event.getSymbol(),
                            afterEndOfLine,
                            false));
        } catch (PsiInvalidElementAccessException e) {
            LOG.warn("Element access failed", e);
        }
    }

    @NotNull
    public Map<PsiFile, List<Problem>> getProblems() {
        return Collections.unmodifiableMap(problems);
    }

    private void addProblem(final PsiFile psiFile, final Problem problem) {
        List<Problem> problemsForFile = problems.get(psiFile);
        if (problemsForFile == null) {
            problemsForFile = new ArrayList<>();
            problems.put(psiFile, problemsForFile);
        }

        problemsForFile.add(problem);
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

    @NotNull
    private Map<PsiFile, List<Problem>> filesWithProblems(final Set<PsiFile> filesForModule,
                                                          final Map<PsiFile, List<Problem>> moduleFileResults) {
        final Map<PsiFile, List<Problem>> moduleResults = new HashMap<>();
        for (final PsiFile psiFile : filesForModule) {
            final List<Problem> resultsForFile = moduleFileResults.get(psiFile);
            if (resultsForFile != null && !resultsForFile.isEmpty()) {
                moduleResults.put(psiFile, new ArrayList<>(resultsForFile));
            }
        }
        return moduleResults;
    }

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

    @NotNull
    private Position findPosition(final List<Integer> lineLengthCache, final Issue event, final char[] text) {
        if (event.getLine() == 0) {
            return Position.at(event.getColumn());
        } else if (event.getLine() <= lineLengthCache.size()) {
            return Position.at(lineLengthCache.get(event.getLine() - 1) + event.getColumn());
        } else {
            return searchFromEndOfCachedData(lineLengthCache, event, text);
        }
    }

    private char nextCharacter(final char[] text, final int i) {
        if ((i + 1) < text.length) {
            return text[i + 1];
        }
        return '\0';
    }

    @NotNull
    private Position searchFromEndOfCachedData(final List<Integer> lineLengthCache,
                                               final Issue event,
                                               final char[] text) {
        final Position position;
        int offset = lineLengthCache.get(lineLengthCache.size() - 1);
        boolean afterEndOfLine = false;
        int line = lineLengthCache.size();

        int column = 0;
        for (int i = offset; i < text.length; ++i) {
            final char character = text[i];

            // for linefeeds we need to handle CR, LF and CRLF,
            // hence we accept either and only trigger a new
            // line on the LF of CRLF.
            final char nextChar = nextCharacter(text, i);
            if (character == '\n' || (character == '\r' && nextChar != '\n')) {
                ++line;
                ++offset;
                lineLengthCache.add(offset);
                column = 0;
            } else if (character == '\t') {
                column += tabWidth;
                ++offset;
            } else {
                ++column;
                ++offset;
            }

            if (event.getLine() == line && event.getColumn() == column) {
                if (column == 0 && Character.isWhitespace(nextChar)) {
                    afterEndOfLine = true;
                }
                break;
            }
        }

        position = Position.at(offset, afterEndOfLine);
        return position;
    }

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

    private static final class Position {
        private final boolean afterEndOfLine;
        private final int offset;

        public static Position at(final int offset, final boolean afterEndOfLine) {
            return new Position(offset, afterEndOfLine);
        }

        public static Position at(final int offset) {
            return new Position(offset, false);
        }

        private Position(final int offset, final boolean afterEndOfLine) {
            this.offset = offset;
            this.afterEndOfLine = afterEndOfLine;
        }

        private PsiElement element(final PsiFile psiFile) {
            return psiFile.findElementAt(offset);
        }

    }
}
