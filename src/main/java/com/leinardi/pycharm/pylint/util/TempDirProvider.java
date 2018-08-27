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

package com.leinardi.pycharm.pylint.util;

import com.intellij.openapi.components.ServiceKt;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.components.impl.stores.IProjectStore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Locate and/or create temporary directories for use by this plugin.
 */
public class TempDirProvider {
    //    private static final Logger LOG = Logger.getInstance(TempDirProvider.class);
    //
    //    private static final String README_TEMPLATE = "copylibs-readme.template.txt";
    //
    //    public static final String README_FILE = "readme.txt";

    public String forPersistedPsiFile(final PsiFile tempPsiFile) {
        String systemTempDir = System.getProperty("java.io.tmpdir");
        if (OS.isWindows() && driveLetterOf(systemTempDir) != driveLetterOf(pathOf(tempPsiFile))) {
            // Pylint on Windows requires the files to be on the same drive
            final File projectTempDir = temporaryDirectoryLocationFor(tempPsiFile.getProject());
            if (projectTempDir.exists() || projectTempDir.mkdirs()) {
                projectTempDir.deleteOnExit();
                return projectTempDir.getAbsolutePath();
            }
        }
        return systemTempDir;
    }

    @NotNull
    private File temporaryDirectoryLocationFor(final Project project) {
        return getIdeaFolder(project).map(vf -> new File(vf.getPath(), "pylintpylint.tmp"))
                .orElse(new File(project.getBasePath(), "pylintpylint.tmp"));
    }

    Optional<VirtualFile> getIdeaFolder(@NotNull final Project pProject) {
        final IProjectStore projectStore = (IProjectStore) ServiceKt.getStateStore(pProject);
        if (projectStore.getStorageScheme() == StorageScheme.DIRECTORY_BASED) {
            final VirtualFile ideaStorageDir = pProject.getBaseDir().findChild(Project.DIRECTORY_STORE_FOLDER);
            if (ideaStorageDir != null && ideaStorageDir.exists() && ideaStorageDir.isDirectory()) {
                return Optional.of(ideaStorageDir);
            }
        }
        return Optional.empty();
    }

    private char driveLetterOf(final String windowsPath) {
        if (windowsPath != null && windowsPath.length() > 0) {
            final Path normalisedPath = Paths.get(windowsPath).normalize().toAbsolutePath();
            return normalisedPath.toFile().toString().charAt(0);
        }
        return '?';
    }

    private String pathOf(@NotNull final PsiFile file) {
        return virtualFileOf(file).map(VirtualFile::getPath).orElseThrow(() -> new IllegalStateException("PSIFile " +
                "does not have associated virtual file: " + file));
    }

    private Optional<VirtualFile> virtualFileOf(final PsiFile file) {
        return Optional.ofNullable(file.getVirtualFile());
    }

    //    /**
    //     * Locate the directory for storing libraries from the project directory in order to prevent their getting
    // locked
    //     * by our classloaders. The directory will be created if it does not exist. It should persist after PyCharm
    // is closed,
    //     * because copying the libraries is potentially time-consuming (so, no deleteOnExit()!).
    //     *
    //     * @param pProject the current project
    //     * @return the existing directory, or an empty Optional if such could not be made available
    //     */
    //    public Optional<File> forCopiedLibraries(@NotNull final Project pProject) {
    //        Optional<File> result = Optional.empty();
    //        try {
    //            final File tempDir = determineCopiedLibrariesDir(pProject);
    //            if (tempDir.mkdir()) {
    //                putReadmeFile(pProject, tempDir);
    //            }
    //            if (tempDir.isDirectory()) {
    //                result = Optional.of(tempDir);
    //            }
    //        } catch (IOException | URISyntaxException | RuntimeException e) {
    //            LOG.warn("Unable to create suitable temporary directory. Library unlock unavailable.", e);
    //        }
    //        return result;
    //    }
    //
    //    @NotNull
    //    private File determineCopiedLibrariesDir(@NotNull final Project pProject) {
    //        File result = getIdeaFolder(pProject).map(pVirtualFile -> new File(pVirtualFile.getPath(),
    //                "pylintidea-libs")).orElseGet(() -> new File(System.getProperty("java.io.tmpdir"), "csi-" +
    //                projectUnique(pProject) + "-libs"));
    //        return result;
    //    }
    //
    //    @NotNull
    //    private String projectUnique(@NotNull final Project pProject) {
    //        return pProject.getLocationHash().replaceAll(" ", "_");
    //    }
    //
    //    private void putReadmeFile(@NotNull final Project pProject, @NotNull final File pTempDir)
    //            throws URISyntaxException, IOException {
    //        final Path tempDir = Paths.get(pTempDir.toURI());
    //        final Path readme = tempDir.resolve(README_FILE);
    //        if (!Files.isRegularFile(readme)) {
    //            String s = readTemplate();
    //            if (s != null) {
    //                s = MessageFormat.format(s, pProject.getName(), PylintPlugin.ID_PLUGIN);
    //                s = s.replaceAll("[\r\n]+", System.lineSeparator());
    //                Files.write(readme, s.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
    //            }
    //        }
    //    }
    //
    //    @Nullable
    //    private String readTemplate() throws IOException {
    //        String result = null;
    //        InputStream is = null;
    //        try {
    //            is = getClass().getResourceAsStream(README_TEMPLATE);
    //            result = IOUtils.toString(is, StandardCharsets.UTF_8);
    //        } finally {
    //            IOUtils.closeQuietly(is);
    //        }
    //        return result;
    //    }
    //
    //    public void deleteCopiedLibrariesDir(@NotNull final Project pProject) {
    //        try {
    //            final File dir = determineCopiedLibrariesDir(pProject);
    //            if (dir.isDirectory()) {
    //                FileUtils.deleteQuietly(dir);
    //            }
    //        } catch (RuntimeException e) {
    //            // ignore
    //        }
    //    }
}
