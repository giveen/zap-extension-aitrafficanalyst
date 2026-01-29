package org.zaproxy.zap.extension.aitrafficanalyst.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpSender;
import org.parosproxy.paros.model.Model;
import org.zaproxy.zap.view.messagecontainer.http.HttpMessageContainer;
import org.zaproxy.zap.view.popup.PopupMenuItemHttpMessageContainer;
import org.zaproxy.zap.extension.aitrafficanalyst.ExtensionAiAnalyst;

import javax.swing.JOptionPane;

/**
 * A pop up menu item shown in components that contain HTTP messages.
 */
public class AnalystPopupMenu extends PopupMenuItemHttpMessageContainer {

    private static final long serialVersionUID = 1L;

    private transient ExtensionAiAnalyst extension;
    private static final Logger LOGGER = LogManager.getLogger(AnalystPopupMenu.class);

    public AnalystPopupMenu(ExtensionAiAnalyst ext) {
        super(org.parosproxy.paros.Constant.messages.getString("aitrafficanalyst.menu.analyze"));
        this.extension = ext;
    }

    @Override
    public void performAction(HttpMessage msg) {
        try {
            if (msg != null) {
                String url = msg.getRequestHeader().getURI().toString();
                String method = msg.getRequestHeader().getMethod();
                // Show Thinking state in the panel
                    if (extension.getAnalystPanel() != null) {
                    extension.getAnalystPanel().setTabFocus();
                    String modelName = this.extension.getOptions() != null ? this.extension.getOptions().getModelName() : "llama3:70b";
                    extension.getAnalystPanel().updateAnalysis(url, "Thinking... (Querying " + modelName + ")...");
                }

                LOGGER.info("Sending analysis request to Ollama for: " + url);

                // 2. Run in Background Thread (Do not freeze ZAP UI)
                new Thread(() -> {
                    try {
                        // Get config from Options
                        String modelName = this.extension.getOptions() != null ? this.extension.getOptions().getModelName() : "llama3:70b";
                        String ollamaUrl = this.extension.getOptions() != null ? this.extension.getOptions().getOllamaUrl() : "http://localhost:11434/api/generate";

                        // Notify panel we're sending a live request
                        if (extension.getAnalystPanel() != null) {
                            extension.getAnalystPanel().updateAnalysis(url, "📡 Sending live request to capture fresh response...");
                        }

                        // 1. Perform the live request
                        HttpSender sender = new HttpSender(HttpSender.MANUAL_REQUEST_INITIATOR);
                        HttpMessage liveMsg = msg.cloneAll();
                        sender.sendAndReceive(liveMsg);

                        // Show thinking state
                        if (extension.getAnalystPanel() != null) {
                            extension.getAnalystPanel().updateAnalysis(url, "🤖 Thinking... (Querying " + modelName + " with live data)...");
                        }

                        // 2. Build the prompt using liveMsg
                        StringBuilder liveSb = new StringBuilder();
                        String userPrompt = (this.extension.getOptions() != null) ? this.extension.getOptions().getSystemPrompt() : null;
                        if (userPrompt != null && !userPrompt.isEmpty()) {
                            liveSb.append(userPrompt).append("\n\n");
                        } else {
                            liveSb.append("You are a security expert. Analyze this HTTP request for vulnerabilities.\n\n");
                        }

                        liveSb.append("--- LIVE REQUEST ---\n");
                        liveSb.append(liveMsg.getRequestHeader().toString()).append("\n");
                        if (liveMsg.getRequestBody().length() > 0) {
                            liveSb.append(liveMsg.getRequestBody().toString()).append("\n");
                        }

                        liveSb.append("\n--- LIVE RESPONSE ---\n");
                        liveSb.append(liveMsg.getResponseHeader().toString()).append("\n");
                        if (liveMsg.getResponseBody().length() > 0) {
                            String body = liveMsg.getResponseBody().toString();
                            if (body.length() > 5000) {
                                body = body.substring(0, 5000) + "... [TRUNCATED]";
                            }
                            liveSb.append(body).append("\n");
                        }

                        liveSb.append("\n--- END CONVERSATION ---\n");
                        liveSb.append("Analyze the interaction. Did the response confirm any vulnerabilities suggested by the request?");
                        String livePrompt = liveSb.toString();

                        // 3. Query the AI using liveMsg data
                        org.zaproxy.zap.extension.aitrafficanalyst.ai.OllamaClient ai = new org.zaproxy.zap.extension.aitrafficanalyst.ai.OllamaClient(ollamaUrl);
                        String result = ai.query(modelName, livePrompt);

                        // Update panel on EDT
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            if (extension.getAnalystPanel() != null) {
                                extension.getAnalystPanel().updateAnalysis(url, result);
                            }
                        });
                    } catch (Exception e) {
                        LOGGER.error("Ollama Analysis Failed", e);
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            if (extension.getAnalystPanel() != null) {
                                extension.getAnalystPanel().updateAnalysis(url, "❌ Error: " + e.getMessage());
                            }
                        });
                    }
                }).start();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve HTTP message", e);
        }
    }

    @Override
    public boolean isEnableForInvoker(Invoker invoker, HttpMessageContainer httpMessageContainer) {
        return true;
    }

    @Override
    public boolean isSafe() {
        return true;
    }
}
