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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.OptionsParam;
import org.parosproxy.paros.view.AbstractParamPanel;
import org.zaproxy.zap.extension.aitrafficanalyst.AnalystOptions;
import org.zaproxy.zap.extension.aitrafficanalyst.ExtensionAiAnalyst;

public class AnalystOptionsPanel extends AbstractParamPanel {

    private static final long serialVersionUID = 1L;
    private JLabel lblLlmStatus;
    private JComboBox<String> cboRole;
    private JButton btnAddRole;
    private JButton btnDeleteRole;
    private JTextArea txtRolePrompt;
    private transient ExtensionAiAnalyst extension;

    // Local edits are staged here and only persisted on Save.
    private final transient Map<String, String> editedRoles = new LinkedHashMap<>();
    private String editedActiveRole;
    private boolean suppressRoleEvents;

    public AnalystOptionsPanel(ExtensionAiAnalyst extension) {
        this.extension = extension;
    }

    public void init() {
        initGUI();
    }

    private void initGUI() {
        this.setLayout(new GridBagLayout());
        this.setName("AI Traffic Analyst"); // Name in the tree on the left

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblLlmConfig =
            new JLabel(
                org.parosproxy.paros.Constant.messages.getString(
                    "aitrafficanalyst.options.llmConfig"));

        JTextArea txtLlmConfig =
            new JTextArea(
                org.parosproxy.paros.Constant.messages.getString(
                    "aitrafficanalyst.options.llmConfig.desc"));
        txtLlmConfig.setEditable(false);
        txtLlmConfig.setLineWrap(true);
        txtLlmConfig.setWrapStyleWord(true);
        txtLlmConfig.setOpaque(false);
        txtLlmConfig.setFocusable(false);

        lblLlmStatus = new JLabel();

        // Row 1: LLM add-on guidance
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(lblLlmConfig, gbc);

        gbc.gridy = 1;
        this.add(txtLlmConfig, gbc);

        gbc.gridy = 2;
        this.add(lblLlmStatus, gbc);

        gbc.gridwidth = 1;
        gbc.weightx = 0.0;

        // Row 3: Role selector + add/delete
        JLabel lblRole =
                new JLabel(
                        org.parosproxy.paros.Constant.messages.getString(
                                "aitrafficanalyst.options.role"));
        cboRole = new JComboBox<>();

        btnAddRole =
                new JButton(
                        org.parosproxy.paros.Constant.messages.getString(
                                "aitrafficanalyst.options.role.add"));
        btnDeleteRole =
                new JButton(
                        org.parosproxy.paros.Constant.messages.getString(
                                "aitrafficanalyst.options.role.delete"));

        JPanel rolePanel = new JPanel(new GridBagLayout());
        GridBagConstraints roleGbc = new GridBagConstraints();
        roleGbc.gridx = 0;
        roleGbc.weightx = 1.0;
        roleGbc.fill = GridBagConstraints.HORIZONTAL;
        rolePanel.add(cboRole, roleGbc);

        roleGbc.gridx = 1;
        roleGbc.weightx = 0.0;
        roleGbc.insets = new Insets(0, 4, 0, 0);
        rolePanel.add(btnAddRole, roleGbc);

        roleGbc.gridx = 2;
        rolePanel.add(btnDeleteRole, roleGbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        this.add(lblRole, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(rolePanel, gbc);

        // Row 4: Role Prompt Label + TextArea
        JLabel lblPrompt =
                new JLabel(
                        org.parosproxy.paros.Constant.messages.getString(
                                "aitrafficanalyst.options.role.prompt"));
        txtRolePrompt = new JTextArea(8, 60);
        JScrollPane promptScroll = new JScrollPane(txtRolePrompt);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(lblPrompt, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        this.add(promptScroll, gbc);

        // Row 3: Reset to Defaults button
        JButton btnReset =
                new JButton(
                        org.parosproxy.paros.Constant.messages.getString(
                                "aitrafficanalyst.options.reset"));
        btnReset.addActionListener(
                e -> {
                    int confirm =
                            JOptionPane.showConfirmDialog(
                                    this,
                                    "Are you sure you want to reset all settings to their original defaults?",
                                    "Confirm Reset",
                                    JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        // 1. Reset the underlying data
                        OptionsParam optionsParam = Model.getSingleton().getOptionsParam();
                        AnalystOptions options = optionsParam.getParamSet(AnalystOptions.class);
                        options.resetToDefaults();

                        // 2. Refresh the UI components to show the new values
                        initParam(optionsParam);
                    }
                });

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        this.add(btnReset, gbc);

        // Spacer to push everything up (now row 5)
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 0.0;
        this.add(new JPanel(), gbc);

        // Wire role UI actions.
        cboRole.addActionListener(
                e -> {
                    if (suppressRoleEvents) {
                        return;
                    }
                    String selected = (String) cboRole.getSelectedItem();
                    if (selected == null) {
                        return;
                    }
                    stageCurrentRolePrompt();
                    editedActiveRole = selected;
                    txtRolePrompt.setText(editedRoles.getOrDefault(selected, ""));
                    updateRoleButtonsState();
                });

        btnAddRole.addActionListener(
                e -> {
                    String roleName =
                            JOptionPane.showInputDialog(
                                    this,
                                    org.parosproxy.paros.Constant.messages.getString(
                                            "aitrafficanalyst.options.role.add.prompt"),
                                    org.parosproxy.paros.Constant.messages.getString(
                                            "aitrafficanalyst.options.role.add.title"),
                                    JOptionPane.QUESTION_MESSAGE);
                    if (roleName == null) {
                        return;
                    }
                    roleName = roleName.trim();
                    if (roleName.isEmpty()) {
                        return;
                    }
                    if (editedRoles.containsKey(roleName)) {
                        JOptionPane.showMessageDialog(
                                this,
                                org.parosproxy.paros.Constant.messages.getString(
                                        "aitrafficanalyst.options.role.add.duplicate"),
                                "",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    stageCurrentRolePrompt();
                    editedRoles.put(roleName, "");
                    rebuildRoleCombo(roleName);
                });

        btnDeleteRole.addActionListener(
                e -> {
                    String selected = (String) cboRole.getSelectedItem();
                    if (selected == null) {
                        return;
                    }
                    if (AnalystOptions.DEFAULT_ROLE.equals(selected)) {
                        return;
                    }
                    int confirm =
                            JOptionPane.showConfirmDialog(
                                    this,
                                    org.parosproxy.paros.Constant.messages.getString(
                                            "aitrafficanalyst.options.role.delete.confirm"),
                                    org.parosproxy.paros.Constant.messages.getString(
                                            "aitrafficanalyst.options.role.delete.title"),
                                    JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                    editedRoles.remove(selected);
                    if (Objects.equals(editedActiveRole, selected)) {
                        editedActiveRole = AnalystOptions.DEFAULT_ROLE;
                    }
                    rebuildRoleCombo(editedActiveRole);
                });
    }

    private void updateRoleButtonsState() {
        String selected = (String) cboRole.getSelectedItem();
        btnDeleteRole.setEnabled(selected != null && !AnalystOptions.DEFAULT_ROLE.equals(selected));
    }

    private void stageCurrentRolePrompt() {
        if (editedActiveRole == null) {
            return;
        }
        editedRoles.put(editedActiveRole, txtRolePrompt.getText());
    }

    private void rebuildRoleCombo(String roleToSelect) {
        suppressRoleEvents = true;
        cboRole.removeAllItems();
        for (String role : editedRoles.keySet()) {
            cboRole.addItem(role);
        }
        if (roleToSelect != null && editedRoles.containsKey(roleToSelect)) {
            cboRole.setSelectedItem(roleToSelect);
            editedActiveRole = roleToSelect;
            txtRolePrompt.setText(editedRoles.getOrDefault(roleToSelect, ""));
        } else {
            cboRole.setSelectedItem(AnalystOptions.DEFAULT_ROLE);
            editedActiveRole = AnalystOptions.DEFAULT_ROLE;
            txtRolePrompt.setText(editedRoles.getOrDefault(AnalystOptions.DEFAULT_ROLE, ""));
        }
        updateRoleButtonsState();
        suppressRoleEvents = false;
    }

    @Override
    public void initParam(Object obj) {
        OptionsParam optionsParam = (OptionsParam) obj;
        AnalystOptions options = optionsParam.getParamSet(AnalystOptions.class);

        // Show current LLM add-on configuration status (best-effort).
        String llmStatusText;
        if (extension == null || extension.getLlmClient() == null) {
            llmStatusText =
                org.parosproxy.paros.Constant.messages.getString(
                    "aitrafficanalyst.options.llmConfig.status.unavailable");
        } else if (extension.getLlmClient().isConfigured()) {
            llmStatusText =
                org.parosproxy.paros.Constant.messages.getString(
                    "aitrafficanalyst.options.llmConfig.status.configured");
        } else {
            String issue = extension.getLlmClient().getCommsIssue();
            if (issue != null && !issue.trim().isEmpty()) {
            llmStatusText =
                org.parosproxy.paros.Constant.messages.getString(
                        "aitrafficanalyst.options.llmConfig.status.notConfigured")
                    + " "
                    + issue;
            } else {
            llmStatusText =
                org.parosproxy.paros.Constant.messages.getString(
                    "aitrafficanalyst.options.llmConfig.status.notConfigured");
            }
        }
        if (lblLlmStatus != null) {
            lblLlmStatus.setText(llmStatusText);
        }

        // Stage roles locally (do not persist until Save).
        editedRoles.clear();
        editedRoles.putAll(options.getRoles());
        editedActiveRole = options.getActiveRole();
        if (editedRoles.isEmpty()) {
            editedRoles.put(
                    AnalystOptions.DEFAULT_ROLE,
                    options.getRolePrompt(AnalystOptions.DEFAULT_ROLE));
        }
        if (editedActiveRole == null || !editedRoles.containsKey(editedActiveRole)) {
            editedActiveRole = AnalystOptions.DEFAULT_ROLE;
        }
        rebuildRoleCombo(editedActiveRole);
    }

    @Override
    public void saveParam(Object obj) throws Exception {
        OptionsParam optionsParam = (OptionsParam) obj;
        AnalystOptions options = optionsParam.getParamSet(AnalystOptions.class);

        // Persist current role prompt before saving.
        stageCurrentRolePrompt();
        if (editedRoles.isEmpty()) {
            editedRoles.put(
                    AnalystOptions.DEFAULT_ROLE,
                    options.getRolePrompt(AnalystOptions.DEFAULT_ROLE));
        }
        if (editedActiveRole == null || !editedRoles.containsKey(editedActiveRole)) {
            editedActiveRole = AnalystOptions.DEFAULT_ROLE;
        }

        options.setRoles(editedRoles);
        options.setActiveRole(editedActiveRole);
    }

    @Override
    public String getHelpIndex() {
        return "aitrafficanalyst.options";
    }
}
