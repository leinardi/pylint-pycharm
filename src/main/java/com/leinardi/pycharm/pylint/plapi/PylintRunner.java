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
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.exception.PylintPluginException;
import com.leinardi.pycharm.pylint.exception.PylintPluginParseException;
import com.leinardi.pycharm.pylint.exception.PylintToolException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.Okio;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

public class PylintRunner {
    private final PylintPlugin plugin;
    private final Set<String> filesToScan;

    public PylintRunner(PylintPlugin plugin, Set<String> filesToScan) {
        this.plugin = plugin;
        this.filesToScan = filesToScan;
    }

    public List<Issue> scan() throws InterruptedIOException {
        if (filesToScan.isEmpty()) {
            throw new PylintPluginException("Illegal state: filesToScan is empty");
        }

        GeneralCommandLine generalCommandLine = new GeneralCommandLine("pylint");
        generalCommandLine.setCharset(Charset.forName("UTF-8"));
        generalCommandLine.addParameter("-f");
        generalCommandLine.addParameter("json");
        for (String file : filesToScan) {
            generalCommandLine.addParameter(file);
        }

        //        GeneralCommandLine generalCommandLine = new GeneralCommandLine("cat");
        //        generalCommandLine.setCharset(Charset.forName("UTF-8"));
        //        generalCommandLine.addParameter("pl.txt");

        generalCommandLine.setWorkDirectory(plugin.getProject().getBasePath());
        //generalCommandLine.getCommandLineString();
        final Process process;
        try {
            process = generalCommandLine.createProcess();
            //            process.waitFor();
            Moshi moshi = new Moshi.Builder().build();
            Type type = Types.newParameterizedType(List.class, Issue.class);
            JsonAdapter<List<Issue>> adapter = moshi.adapter(type);
            return adapter.fromJson(Okio.buffer(Okio.source(process.getInputStream())));
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            throw new PylintPluginParseException(e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new PylintToolException("Error creating Pylint process", e);
        }
    }
}
