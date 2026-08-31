/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2026 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.aitrafficanalyst.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CustomAnalysisDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private JCheckBox chkIncludeRequest;
    private JCheckBox chkIncludeResponse;
    private JTextArea customPromptArea;
    private boolean confirmed = false;

    @SuppressWarnings("this-escape")
    public CustomAnalysisDialog(Frame owner) {
        super(owner, "Custom AI Analysis", true);

        setLayout(new BorderLayout(10, 10));
        setSize(400, 300);
        setLocationRelativeTo(owner);

        JPanel optionsPanel = new JPanel(new GridLayout(1, 2));
        chkIncludeRequest = new JCheckBox("Include Request", true);
        chkIncludeResponse = new JCheckBox("Include Response", true);
        optionsPanel.add(chkIncludeRequest);
        optionsPanel.add(chkIncludeResponse);

        customPromptArea =
                new JTextArea(
                        "Enter specific instructions (e.g., 'Focus only on the set-cookie header')...");
        customPromptArea.setLineWrap(true);
        customPromptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(customPromptArea);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton("Cancel");
        JButton btnAnalyze = new JButton("Analyze");

        btnCancel.addActionListener(e -> dispose());
        btnAnalyze.addActionListener(
                e -> {
                    confirmed = true;
                    dispose();
                });

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnAnalyze);

        add(optionsPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        JComponent content = (JComponent) getContentPane();
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean includeRequest() {
        return chkIncludeRequest.isSelected();
    }

    public boolean includeResponse() {
        return chkIncludeResponse.isSelected();
    }

    public String getCustomPrompt() {
        return customPromptArea.getText();
    }
}
