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

package com.leinardi.pycharm.pylint.plapi;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.jetbrains.python.sdk.PySdkUtil;
import com.jetbrains.python.sdk.PythonEnvUtil;
import com.leinardi.pycharm.pylint.PylintBundle;
import com.leinardi.pycharm.pylint.PylintConfigService;
import com.leinardi.pycharm.pylint.exception.PylintPluginException;
import com.leinardi.pycharm.pylint.exception.PylintPluginParseException;
import com.leinardi.pycharm.pylint.exception.PylintToolException;
import com.leinardi.pycharm.pylint.util.Notifications;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.Okio;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PylintRunner {
    private static final String ENV_KEY_VIRTUAL_ENV = "VIRTUAL_ENV";
    private static final String ENV_KEY_PATH = "PATH";
    private static final String ENV_KEY_PYTHONHOME = "PYTHONHOME";

    private PylintRunner() {
    }

    public static boolean isPathToPylintValid(String pathToPylint, Project project) {
        if (pathToPylint.startsWith(File.separator)) {
            VirtualFile pylintFile = LocalFileSystem.getInstance().findFileByPath(pathToPylint);
            if (pylintFile == null || !pylintFile.exists()) {
                return false;
            }
        }
        GeneralCommandLine cmd = getPylintCommandLine(project, pathToPylint);
        cmd.addParameter("--help-msg");
        cmd.addParameter("E1101");
        final Process process;
        try {
            process = cmd.createProcess();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }
    }

    public static boolean isPylintAvailable(Project project) {
        PylintConfigService pylintConfigService = PylintConfigService.getInstance(project);
        if (pylintConfigService == null) {
            throw new IllegalStateException("PylintConfigService is null");
        }
        return isPathToPylintValid(pylintConfigService.getPylintPath(), project);
    }

    private static String getPylintrcFile(Project project, String pathToPylintrcFile) throws PylintPluginException {
        if (pathToPylintrcFile.isEmpty()) {
            return "";
        } else if (!pathToPylintrcFile.startsWith(File.separator)) {
            pathToPylintrcFile = project.getBasePath() + File.separator + pathToPylintrcFile;
        }

        VirtualFile pylintrcFile = LocalFileSystem.getInstance().findFileByPath(pathToPylintrcFile);
        if (pylintrcFile == null || !pylintrcFile.exists()) {
            throw new PylintPluginException("pylintrc file is not valid. File does not exist or can't be read.");
        }

        return pathToPylintrcFile;
    }

    public static String tryToFindPylintPath() {
        GeneralCommandLine cmd = new GeneralCommandLine("which");
        cmd.addParameter(PylintBundle.message("config.pylint.path.default"));
        final Process process;
        try {
            process = cmd.createProcess();
            Optional<String> path = new BufferedReader(
                    new InputStreamReader(cmd.createProcess().getInputStream(), UTF_8))
                    .lines()
                    .findFirst();
            process.waitFor();
            if (process.exitValue() != 0 || !path.isPresent()) {
                return "";
            }
            return path.get();
        } catch (ExecutionException | InterruptedException e) {
            return "";
        }
    }

    public static List<Issue> scan(Project project, Set<String> filesToScan) throws InterruptedIOException {
        if (!isPylintAvailable(project)) {
            Notifications.showPylintNotAvailable(project);
            return Collections.emptyList();
        }
        PylintConfigService pylintConfigService = PylintConfigService.getInstance(project);
        if (filesToScan.isEmpty()) {
            throw new PylintPluginException("Illegal state: filesToScan is empty");
        }
        if (pylintConfigService == null) {
            throw new PylintPluginException("Illegal state: pylintConfigService is null");
        }

        String pathToPylint = pylintConfigService.getPylintPath();
        if (pathToPylint.isEmpty()) {
            throw new PylintToolException("Path to Pylint executable not set (check Plugin Settings)");
        }

        String pathToPylintrcFile = getPylintrcFile(project, pylintConfigService.getPylintrcPath());

        GeneralCommandLine cmd = getPylintCommandLine(project, pathToPylint);

        cmd.setCharset(Charset.forName("UTF-8"));
        cmd.addParameter("-f");
        cmd.addParameter("json");

        injectEnvironmentVariables(project, cmd);

        if (!pathToPylintrcFile.isEmpty()) {
            cmd.addParameter("--rcfile");
            cmd.addParameter(pathToPylintrcFile);
        }

        String[] args = pylintConfigService.getPylintArguments().split(" ", -1);
        for (String arg : args) {
            if (!StringUtil.isEmpty(arg)) {
                cmd.addParameter(arg);
            }
        }

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
            //TODO check stderr for errors
            if (checkIfInputStreamIsEmpty(inputStream)) {
                return new ArrayList<>();
            } else {
                return adapter.fromJson(Okio.buffer(Okio.source(inputStream)));
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            throw new PylintPluginParseException(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new PylintToolException("Error creating Pylint process", e);
        }
    }

    private static GeneralCommandLine getPylintCommandLine(Project project, String pathToPylint) {
        GeneralCommandLine cmd;
        String interpreterPath = getInterpreterPath(project);
        if (interpreterPath == null) {
            cmd = new GeneralCommandLine(pathToPylint);
        } else {
            cmd = new GeneralCommandLine(interpreterPath);
            cmd.addParameter(pathToPylint);
        }
        return cmd;
    }

    private static boolean checkIfInputStreamIsEmpty(InputStream inputStream) throws IOException {
        inputStream.mark(1);
        int data = inputStream.read();
        inputStream.reset();
        return data == -1;
    }

    private static String getInterpreterPath(Project project) {
        // I can't use this until I figure out how to install Pylint in the venv via PyCharm
        //        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
        //        if (projectSdk != null) {
        //            VirtualFile homeDirectory = projectSdk.getHomeDirectory();
        //            return homeDirectory == null ? null : homeDirectory.getPath();
        //        }
        return null;
    }

    private static void injectEnvironmentVariables(Project project, GeneralCommandLine cmd) {
        String interpreterPath = getInterpreterPath(project);
        Map<String, String> extraEnv = null;
        Map<String, String> systemEnv = System.getenv();
        Map<String, String> expandedCmdEnv = PySdkUtil.mergeEnvVariables(systemEnv, cmd.getEnvironment());
        if (isVenv(interpreterPath)) {
            String venvPath = PathUtil.getParentPath(PathUtil.getParentPath(interpreterPath));
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

    private static boolean isVenv(@Nullable String interpreterPath) {
        return interpreterPath != null && interpreterPath.contains(File.separator + "venv" + File.separator);
    }
}
