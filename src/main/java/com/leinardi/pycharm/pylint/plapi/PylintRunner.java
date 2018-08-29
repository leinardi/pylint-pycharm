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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.leinardi.pycharm.pylint.PylintConfigService;
import com.leinardi.pycharm.pylint.exception.PylintPluginException;
import com.leinardi.pycharm.pylint.exception.PylintPluginParseException;
import com.leinardi.pycharm.pylint.exception.PylintToolException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PylintRunner {
    private PylintRunner() {
    }

    public static boolean isPathToPylintValid(String pathToPylint) {
        if (pathToPylint.startsWith(File.separator)) {
            VirtualFile pylintFile = LocalFileSystem.getInstance().findFileByPath(pathToPylint);
            if (pylintFile == null || !pylintFile.exists()) {
                return false;
            }
        }
        GeneralCommandLine generalCommandLine = new GeneralCommandLine(pathToPylint);
        generalCommandLine.addParameter("--help-msg");
        generalCommandLine.addParameter("E1101");
        final Process process;
        try {
            process = generalCommandLine.createProcess();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }
    }

    public static List<Issue> scan(Project project, Set<String> filesToScan) throws InterruptedIOException {
        PylintConfigService pylintConfigService = PylintConfigService.getInstance(project);
        if (filesToScan.isEmpty()) {
            throw new PylintPluginException("Illegal state: filesToScan is empty");
        }
        if (pylintConfigService == null) {
            throw new PylintPluginException("Illegal state: pylintConfigService is null");
        }

        String pathToPylint = pylintConfigService.getPathToPylint();
        if (pathToPylint.isEmpty()) {
            throw new PylintToolException("Path to Pylint executable not set (check Plugin Settings)");
        }
        GeneralCommandLine generalCommandLine = new GeneralCommandLine(pathToPylint);
        generalCommandLine.setCharset(Charset.forName("UTF-8"));
        generalCommandLine.addParameter("-f");
        generalCommandLine.addParameter("json");
        for (String file : filesToScan) {
            generalCommandLine.addParameter(file);
        }

        generalCommandLine.setWorkDirectory(project.getBasePath());
        final Process process;
        try {
            process = generalCommandLine.createProcess();
            Moshi moshi = new Moshi.Builder().build();
            Type type = Types.newParameterizedType(List.class, Issue.class);
            JsonAdapter<List<Issue>> adapter = moshi.adapter(type);
            InputStream inputStream = process.getInputStream();
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

    private static boolean checkIfInputStreamIsEmpty(InputStream inputStream) throws IOException {
        inputStream.mark(1);
        int data = inputStream.read();
        inputStream.reset();
        return data == -1;
    }
}
