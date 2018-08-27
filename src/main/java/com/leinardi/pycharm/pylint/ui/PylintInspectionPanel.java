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

package com.leinardi.pycharm.pylint.ui;

import com.intellij.util.ui.JBUI;
import com.leinardi.pycharm.pylint.PylintBundle;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * Provides a dummy panel for the inspection configuration.
 */
public final class PylintInspectionPanel extends JPanel {

    /**
     * A text label for a description blurb.
     */
    private final JTextArea descriptionLabel = new JTextArea();

    /**
     * Create a new panel.
     */
    public PylintInspectionPanel() {
        super(new GridBagLayout());

        initialise();
    }

    /**
     * Initialise the view.
     */
    private void initialise() {
        // fake a multi-line label with a text area
        descriptionLabel.setText(PylintBundle.message("config.inspection.description"));
        descriptionLabel.setEditable(false);
        descriptionLabel.setEnabled(false);
        descriptionLabel.setWrapStyleWord(true);
        descriptionLabel.setLineWrap(true);
        descriptionLabel.setOpaque(false);
        descriptionLabel.setDisabledTextColor(descriptionLabel.getForeground());

        final GridBagConstraints descLabelConstraints = new GridBagConstraints(
                0, 0, 3, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.BOTH, JBUI.insets(4), 0, 0);
        add(descriptionLabel, descLabelConstraints);
    }

}
