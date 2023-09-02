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

package com.leinardi.pycharm.pylint.plapi;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.sdk.PySdkUtil;
import com.jetbrains.python.sdk.PythonEnvUtil;
import com.leinardi.pycharm.pylint.PylintConfigService;
import com.leinardi.pycharm.pylint.exception.PylintPluginException;
import com.leinardi.pycharm.pylint.exception.PylintPluginParseException;
import com.leinardi.pycharm.pylint.exception.PylintToolException;
import com.leinardi.pycharm.pylint.util.FileTypes;
import com.leinardi.pycharm.pylint.util.Notifications;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.Okio;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PylintRunner {
    public static final String PYLINT_PACKAGE_NAME = "pylint";
    private static final String PYLINT_EXECUTABLE_NAME = PYLINT_PACKAGE_NAME + (OS.isWindows() ? ".exe" : "");
    private static final Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(PylintRunner.class);
    private static final String ENV_KEY_VIRTUAL_ENV = "VIRTUAL_ENV";
    private static final String ENV_KEY_PATH = "PATH";
    private static final String ENV_KEY_PYTHONHOME = "PYTHONHOME";
    private static final String WHICH_EXECUTABLE_NAME = OS.isWindows() ? "where" : "which";
    private static final String ACTIVATE_FILE_NAME = OS.isWindows() ? "activate.bat" : "activate";

    private PylintRunner() {
    }

    public static boolean isPylintPathValid(String pylintPath, Project project) {
        String absolutePath = new File(pylintPath).getAbsolutePath();
        if (!absolutePath.equals(pylintPath)) {
            pylintPath = project.getBasePath() + File.separator + pylintPath;
        }
        VirtualFile pylintFile = LocalFileSystem.getInstance().findFileByPath(pylintPath);
        if (pylintFile == null || !pylintFile.exists()) {
            LOG.warn("Error while checking Pylint path " + pylintPath + ": null or not exists");
            return false;
        }
        GeneralCommandLine cmd = getPylintCommandLine(project, pylintPath);
        cmd.addParameter("--help-msg");
        cmd.addParameter("E1101");
        final Process process;
        try {
            process = cmd.createProcess();
            process.waitFor();
            String error = new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            if (!StringUtil.isEmpty(error)) {
                LOG.info("Command Line string: " + cmd.getCommandLineString());
                LOG.warn("Messages while checking Pylint path: " + error);
            }
            String output = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            if (!StringUtil.isEmpty(output)) {
                LOG.debug("Pylint path check output: " + output);
            }
            if (process.exitValue() != 0) {
                LOG.info("Command Line string: " + cmd.getCommandLineString());
                LOG.warn("Pylint path check process.exitValue: " + process.exitValue());
                return false;
            } else {
                return true;
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.info("Command Line string: " + cmd.getCommandLineString());
            LOG.warn("Error while checking Pylint path", e);
            return false;
        }
    }

    public static String getPylintPath(Project project) {
        return getPylintPath(project, true);
    }

    public static String getPylintPath(Project project, boolean checkConfigService) {
        PylintConfigService pylintConfigService = PylintConfigService.getInstance(project);
        if (checkConfigService) {
            if (pylintConfigService == null) {
                throw new IllegalStateException("PylintConfigService is null");
            }

            String pylintPath = pylintConfigService.getCustomPylintPath();
            if (!pylintPath.isEmpty()) {
                return pylintPath;
            }
        }

        VirtualFile interpreterFile = getInterpreterFile(project);
        if (isVenv(interpreterFile)) {
            VirtualFile pylintFile = LocalFileSystem.getInstance()
                    .findFileByPath(interpreterFile.getParent().getPath() + File.separator + PYLINT_EXECUTABLE_NAME);
            if (pylintFile != null && pylintFile.exists()) {
                return pylintFile.getPresentableUrl();
            }
        } else {
            return detectSystemPylintPath();
        }
        return "";
    }

    public static boolean checkPylintAvailable(Project project) {
        return checkPylintAvailable(project, false);
    }

    public static boolean checkPylintAvailable(Project project, boolean showNotifications) {
        String pylintPath = getPylintPath(project);
        boolean isMypyPathValid = !pylintPath.isEmpty() && isPylintPathValid(pylintPath, project);
        if (isMypyPathValid) {
            return true;
        }

        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (projectSdk == null
                || projectSdk.getHomeDirectory() == null
                || !projectSdk.getHomeDirectory().exists()) {
            if (showNotifications) {
                Notifications.showNoPythonInterpreter(project);
            }
        } else if (showNotifications) {
            PyPackageManager pyPackageManager = PyPackageManager.getInstance(projectSdk);
            List<PyPackage> packages = pyPackageManager.getPackages();
            if (packages != null) {
                if (packages.stream().noneMatch(it -> PYLINT_PACKAGE_NAME.equals(it.getName()))) {
                    Notifications.showInstallPylint(project);
                }
            }
        }

        return false;
    }

    private static String getPylintrcFile(Project project, String pylintrcPath) throws PylintPluginException {
        String absolutePath = new File(pylintrcPath).getAbsolutePath();
        if (pylintrcPath.isEmpty()) {
            return "";
        } else if (!absolutePath.equals(pylintrcPath)) {
            pylintrcPath = project.getBasePath() + File.separator + pylintrcPath;
        }

        VirtualFile pylintrcFile = LocalFileSystem.getInstance().findFileByPath(pylintrcPath);
        if (pylintrcFile == null || !pylintrcFile.exists()) {
            throw new PylintPluginException("pylintrc file is not valid. File does not exist or can't be read.");
        }

        return pylintrcPath;
    }

    public static String detectSystemPylintPath() {
        GeneralCommandLine cmd = new GeneralCommandLine(WHICH_EXECUTABLE_NAME);
        cmd.addParameter(PYLINT_EXECUTABLE_NAME);
        final Process process;
        try {
            process = cmd.createProcess();
            Optional<String> path = new BufferedReader(
                    new InputStreamReader(cmd.createProcess().getInputStream(), UTF_8))
                    .lines()
                    .findFirst();
            process.waitFor();
            String error = new BufferedReader(new InputStreamReader(process.getErrorStream(), UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            if (!StringUtil.isEmpty(error)) {
                LOG.info("Command Line string: " + cmd.getCommandLineString());
                LOG.info("Messages while checking Pylint path: " + error);
            }
            if (process.exitValue() != 0 || !path.isPresent()) {
                LOG.info("Command Line string: " + cmd.getCommandLineString());
                LOG.warn("Pylint path detect process.exitValue: " + process.exitValue());
                return "";
            }
            LOG.info("Detected Pylint path: " + path.get());
            return path.get();
        } catch (ExecutionException | InterruptedException e) {
            return "";
        }
    }

    public static List<Issue> scan(Project project, Set<String> filesToScan) throws InterruptedIOException,
            InterruptedException {
        if (!checkPylintAvailable(project, true)) {
            return new ArrayList<>();
        }
        PylintConfigService pylintConfigService = PylintConfigService.getInstance(project);
        if (filesToScan.isEmpty()) {
            throw new PylintPluginException("Illegal state: filesToScan is empty");
        }
        if (pylintConfigService == null) {
            throw new PylintPluginException("Illegal state: pylintConfigService is null");
        }

        String pylintPath = getPylintPath(project);
        if (pylintPath.isEmpty()) {
            throw new PylintToolException("Path to Pylint executable not set (check Plugin Settings)");
        }

        String pylintrcPath = getPylintrcFile(project, pylintConfigService.getPylintrcPath());

        GeneralCommandLine cmd = getPylintCommandLine(project, pylintPath);

        cmd.setCharset(UTF_8);
        cmd.addParameter("-f");
        cmd.addParameter("json");

        injectEnvironmentVariables(project, cmd);

        if (!pylintrcPath.isEmpty()) {
            cmd.addParameter("--rcfile");
            cmd.addParameter(pylintrcPath);
        }

        ParametersList parametersList = cmd.getParametersList();
        parametersList.addParametersString(pylintConfigService.getPylintArguments());

        for (String file : filesToScan) {
            cmd.addParameter(file);
        }

        cmd.setWorkDirectory(project.getBasePath());
        final Process process;

        try {
            process = cmd.createProcess();
            Moshi moshi = new Moshi.Builder().build();
            Type type = Types.newParameterizedType(List.class, Issue.class);
            JsonAdapter<List<Issue>> adapter = moshi.adapter(type);
            InputStream inputStream = process.getInputStream();
            assert (inputStream != null);
            List<Issue> issues;
            if (checkIfInputStreamIsEmpty(inputStream)) {
                issues = new ArrayList<>();
            } else {
                issues = adapter.fromJson(Okio.buffer(Okio.source(inputStream)));
            }
            process.waitFor();

            // Anything equal or bigger than 32 is an abnormal exit code
            // See https://docs.pylint.org/en/1.6.0/run.html#exit-codes
            int exitCode = process.exitValue();
            if (exitCode >= 32) {
                InputStream errStream = process.getErrorStream();
                String detail = new BufferedReader(new InputStreamReader(errStream, UTF_8))
                        .lines().collect(Collectors.joining("\n"));

                Notifications.showPylintAbnormalExit(project, detail);
                throw new PylintToolException("Pylint failed with code " + exitCode);
            }
            return issues;
        } catch (InterruptedIOException e) {
            LOG.info("Command Line string: " + cmd.getCommandLineString());
            throw e;
        } catch (IOException e) {
            LOG.info("Command Line string: " + cmd.getCommandLineString());
            throw new PylintPluginParseException(e.getMessage(), e);
        } catch (ExecutionException e) {
            LOG.info("Command Line string: " + cmd.getCommandLineString());
            throw new PylintToolException("Error creating Pylint process", e);
        }
    }

    private static GeneralCommandLine getPylintCommandLine(Project project, String pylintPath) {
        GeneralCommandLine cmd;
        VirtualFile interpreterFile = getInterpreterFile(project);
        if (interpreterFile == null || FileTypes.isWindowsExecutable(pylintPath)) {
            cmd = new GeneralCommandLine(pylintPath);
        } else {
            cmd = new GeneralCommandLine(interpreterFile.getPath());
            cmd.addParameter(pylintPath);
        }
        return cmd;
    }

    private static boolean checkIfInputStreamIsEmpty(InputStream inputStream) throws IOException {
        inputStream.mark(1);
        int data = inputStream.read();
        inputStream.reset();
        return data == -1;
    }

    @Nullable
    private static VirtualFile getInterpreterFile(Project project) {
        PylintConfigService pylintConfigService = PylintConfigService.getInstance(project);
        if (pylintConfigService == null) {
            throw new IllegalStateException("PylintConfigService is null");
        }
        String customPylintPath = pylintConfigService.getCustomPylintPath();
        if (!customPylintPath.isEmpty()) {
            Path pylintFolder = Path.of(customPylintPath).getParent();
            File file = pylintFolder.resolve("python").toFile();
            if (!file.exists()) {
                return null;
            }

            LocalFileSystem fileSystem = LocalFileSystem.getInstance();
            return fileSystem.findFileByIoFile(file);

        } else {
            Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
            if (projectSdk != null) {
                return projectSdk.getHomeDirectory();
            }
            return null;
        }
    }

    private static void injectEnvironmentVariables(Project project, GeneralCommandLine cmd) {
        VirtualFile interpreterFile = getInterpreterFile(project);
        Map<String, String> extraEnv = null;
        Map<String, String> systemEnv = System.getenv();
        Map<String, String> expandedCmdEnv = PySdkUtil.mergeEnvVariables(systemEnv, cmd.getEnvironment());
        if (isVenv(interpreterFile)) {
            String venvPath = PathUtil.getParentPath(PathUtil.getParentPath(interpreterFile.getPath()));
            extraEnv = new HashMap<>();
            extraEnv.put(ENV_KEY_VIRTUAL_ENV, venvPath);
            if (expandedCmdEnv.containsKey(ENV_KEY_PATH)) {
                PythonEnvUtil.addPathToEnv(expandedCmdEnv, ENV_KEY_PATH, venvPath);
            }
            expandedCmdEnv.remove(ENV_KEY_PYTHONHOME);
        }

        Map<String, String> env = extraEnv != null ? PySdkUtil.mergeEnvVariables(expandedCmdEnv, extraEnv) :
                expandedCmdEnv;
        cmd.withEnvironment(env);
    }

    private static boolean isVenv(@Nullable VirtualFile interpreterFile) {
        return interpreterFile != null && interpreterFile.getParent().findChild(ACTIVATE_FILE_NAME) != null;
    }

}
