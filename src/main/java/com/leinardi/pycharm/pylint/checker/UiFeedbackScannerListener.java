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

import com.intellij.psi.PsiFile;
import com.leinardi.pycharm.pylint.PylintPlugin;
import com.leinardi.pycharm.pylint.exception.PylintPluginException;
import com.leinardi.pycharm.pylint.toolwindow.PylintToolWindowPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;

public class UiFeedbackScannerListener implements ScannerListener {
    private final PylintPlugin plugin;

    public UiFeedbackScannerListener(final PylintPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void scanStarting(final List<PsiFile> filesToScan) {
        SwingUtilities.invokeLater(() -> {
            final PylintToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayInProgress(filesToScan.size());
            }
        });
    }

    @Override
    public void filesScanned(final int count) {
        SwingUtilities.invokeLater(() -> {
            final PylintToolWindowPanel toolWindowPanel = PylintToolWindowPanel.panelFor(plugin.getProject());
            if (toolWindowPanel != null) {
                toolWindowPanel.incrementProgressBarBy(count);
            }
        });
    }

    @Override
    public void scanCompletedSuccessfully(
            final Map<PsiFile, List<Problem>> scanResults) {
        SwingUtilities.invokeLater(() -> {
            final PylintToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayResults(scanResults);
            }
        });
    }

    @Override
    public void scanFailedWithError(final PylintPluginException error) {
        SwingUtilities.invokeLater(() -> {
            final PylintToolWindowPanel toolWindowPanel = toolWindowPanel();
            if (toolWindowPanel != null) {
                toolWindowPanel.displayErrorResult(error);
            }
        });
    }

    @Nullable
    private PylintToolWindowPanel toolWindowPanel() {
        return PylintToolWindowPanel.panelFor(plugin.getProject());
    }
}
