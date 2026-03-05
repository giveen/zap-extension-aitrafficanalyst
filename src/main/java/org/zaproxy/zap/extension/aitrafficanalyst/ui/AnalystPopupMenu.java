package org.zaproxy.zap.extension.aitrafficanalyst.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.awt.Frame;
import javax.swing.JOptionPane;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.aitrafficanalyst.ExtensionAiAnalyst;
import org.zaproxy.zap.view.messagecontainer.http.HttpMessageContainer;
import org.zaproxy.zap.view.popup.PopupMenuItemHttpMessageContainer;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.CustomAnalysisDialog;

/**
 * A pop up menu item shown in components that contain HTTP messages.
 */
public class AnalystPopupMenu extends PopupMenuItemHttpMessageContainer {

    private static final long serialVersionUID = 1L;

    private transient ExtensionAiAnalyst extension;
    private final String expectedMethod; // "GET" or "POST" (or null for any)
    private static final Logger LOGGER = LogManager.getLogger(AnalystPopupMenu.class);

    public AnalystPopupMenu(ExtensionAiAnalyst ext) {
        this(null, ext, null);
    }

    public AnalystPopupMenu(String label, ExtensionAiAnalyst ext) {
        this(label, ext, null);
    }

    public AnalystPopupMenu(String label, ExtensionAiAnalyst ext, String expectedMethod) {
        super(label != null && !label.isEmpty()
            ? label
            : org.parosproxy.paros.Constant.messages.getString("aitrafficanalyst.menu.analyze"));
        this.extension = ext;
        this.expectedMethod = expectedMethod;
        LOGGER.debug(
            "AnalystPopupMenu created with label='{}', expectedMethod='{}'",
            label,
            expectedMethod);
    }

    @Override
    public boolean isSubMenu() {
        return true;
    }

    @Override
    public String getParentMenuName() {
        return Constant.messages.getString("aitrafficanalyst.menu.aiAnalyst");
    }

    @Override
    public int getParentWeight() {
        return 1000;
    }

    @Override
    public void performAction(HttpMessage msg) {
        try {
            if (msg == null) {
                return;
            }

            if ("CUSTOM".equalsIgnoreCase(expectedMethod)) {
                Frame owner =
                        View.getSingleton() != null ? View.getSingleton().getMainFrame() : null;
                CustomAnalysisDialog dialog = new CustomAnalysisDialog(owner);
                dialog.setVisible(true);
                if (dialog.isConfirmed() && extension != null) {
                    extension.analyzeRequestCustom(
                            msg,
                            dialog.getCustomPrompt(),
                            dialog.includeRequest(),
                            dialog.includeResponse());
                }
                return;
            }

            if (extension != null && extension.getAnalystPanel() != null) {
                extension.getAnalystPanel().setLastMessage(msg);
            }

            String actualMethod = msg.getRequestHeader().getMethod();

            // Warn if the user selected the wrong analysis mode for this message's method.
            if (expectedMethod != null
                    && !"CUSTOM".equalsIgnoreCase(expectedMethod)
                    && actualMethod != null
                    && !actualMethod.equalsIgnoreCase(expectedMethod)) {
                String tmpl = Constant.messages.getString("aitrafficanalyst.methodMismatch.msg");
                String msgText =
                        java.text.MessageFormat.format(tmpl, expectedMethod, actualMethod);
                int choice =
                        JOptionPane.showConfirmDialog(
                                View.getSingleton().getMainFrame(),
                                msgText,
                                Constant.messages.getString(
                                        "aitrafficanalyst.methodMismatch.title"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            LOGGER.info(
                    "Sending analysis request to LLM add-on for: {}",
                    msg.getRequestHeader().getURI());

            if (extension != null) {
                extension.analyzeRequest(msg, actualMethod);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to initiate analysis for HTTP message", e);
            javax.swing.SwingUtilities.invokeLater(
                    () -> {
                        if (extension != null && extension.getAnalystPanel() != null) {
                            String errT =
                                    Constant.messages.getString(
                                            "aitrafficanalyst.error.initiating");
                            String errMsg = java.text.MessageFormat.format(errT, e.getMessage());
                            extension.getAnalystPanel().updateAnalysis("(local)", errMsg);
                        }
                    });
        }
    }

    @Override
    public boolean isEnableForInvoker(Invoker invoker, HttpMessageContainer httpMessageContainer) {
        LOGGER.debug("AnalystPopupMenu.isEnableForInvoker called; invoker={}, hasContainer={}",
                invoker == null ? "null" : invoker.name(), httpMessageContainer != null);
        return true;
    }

    @Override
    public boolean isSafe() {
        return true;
    }
}
