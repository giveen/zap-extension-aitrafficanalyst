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
package org.zaproxy.zap.extension.aitrafficanalyst;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.AnalystOptionsPanel;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.AnalystPanel;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.AnalystPopupMenu;

public class ExtensionAiAnalyst extends ExtensionAdaptor {

    public static final String NAME = "ExtensionAiAnalyst";
    private static final Logger LOGGER = LogManager.getLogger(ExtensionAiAnalyst.class);

    private AnalystPanel analystPanel;
    private AnalystOptions options;
    private ExecutorService executor;
    private OkHttpClient httpClient;

    // NEW: Session Memory Buffer (Max 5 items)
    private final List<String> sessionContext = Collections.synchronizedList(new LinkedList<>());
    private static final int MAX_CONTEXT_SIZE = 5;

    public ExtensionAiAnalyst() {
        super(NAME);
    }

    @Override
    public void init() {
        super.init();
        setI18nPrefix("aitrafficanalyst");
        // Register our options class with ZAP's configuration system
        this.options = new AnalystOptions();
        // Create a daemon cached thread pool for background tasks
        this.executor =
                Executors.newCachedThreadPool(
                        new ThreadFactory() {
                            @Override
                            public Thread newThread(Runnable r) {
                                Thread t = new Thread(r);
                                t.setDaemon(true);
                                t.setName("AiAnalyst-worker-" + System.nanoTime());
                                return t;
                            }
                        });
        // Shared OkHttpClient for reuse across Ollama calls
        this.httpClient =
                new OkHttpClient.Builder()
                        .connectTimeout(Duration.ofSeconds(60))
                        .readTimeout(Duration.ofSeconds(120))
                        .writeTimeout(Duration.ofSeconds(60))
                        .build();
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        super.hook(extensionHook);
        LOGGER.info("AI Traffic Analyst: Extension Hook Registered.");

        // Add options to configuration
        extensionHook.addOptionsParamSet(this.options);

        if (getView() != null) {
            // UI Components
            this.analystPanel = new AnalystPanel(this);
            this.analystPanel.init();
            try {
                ImageIcon icon =
                        new ImageIcon(
                                getClass()
                                        .getResource(
                                                "/org/zaproxy/zap/extension/aitrafficanalyst/resources/robot.png"));
                this.analystPanel.setIcon(icon);
            } catch (RuntimeException e) {
                LOGGER.debug("Analyst icon failed to load (runtime issue)", e);
            }
            extensionHook.getHookView().addStatusPanel(this.analystPanel);

            // Register Options Panel
            AnalystOptionsPanel optionsPanel = new AnalystOptionsPanel(this);
            optionsPanel.init();
            extensionHook.getHookView().addOptionPanel(optionsPanel);

            // Register popup menu items.
            // The submenu structure is created by ZAP itself when menu items return isSubMenu() ==
            // true
            // and provide a parent menu name.
            extensionHook
                    .getHookMenu()
                    .addPopupMenuItem(
                            new AnalystPopupMenu(
                                    Constant.messages.getString("aitrafficanalyst.menu.analyzeGet"),
                                    this,
                                    "GET"));
            extensionHook
                    .getHookMenu()
                    .addPopupMenuItem(
                            new AnalystPopupMenu(
                                    Constant.messages.getString(
                                            "aitrafficanalyst.menu.analyzePost"),
                                    this,
                                    "POST"));
        }
    }

    public AnalystOptions getOptions() {
        return this.options;
    }

    public AnalystPanel getAnalystPanel() {
        return this.analystPanel;
    }

    public ExecutorService getExecutor() {
        return this.executor;
    }

    public OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    /**
     * Adds a brief summary of a finding to the session memory. Keeps the list size fixed at
     * MAX_CONTEXT_SIZE.
     */
    public void addSessionInsight(String url, String findingSummary) {
        if (url == null || findingSummary == null) {
            return;
        }

        synchronized (sessionContext) {
            // Format: [http://example.com] found Potential SQLi...
            String entry = String.format("[%s] %s", url, findingSummary);
            sessionContext.add(entry);

            if (sessionContext.size() > MAX_CONTEXT_SIZE) {
                sessionContext.remove(0); // Remove oldest
            }
        }
    }

    /** Returns the accumulated context as a formatted string for the LLM prompt. */
    public String getSessionContextFormatted() {
        synchronized (sessionContext) {
            if (sessionContext.isEmpty()) {
                return "None.";
            }
            return String.join("\n", sessionContext);
        }
    }

    /** Clears session memory (future UI hook). */
    public void clearSessionContext() {
        sessionContext.clear();
    }

    @Override
    public void unload() {
        if (this.executor != null) {
            try {
                this.executor.shutdownNow();
                this.executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (this.httpClient != null) {
            try {
                this.httpClient.connectionPool().evictAll();
                this.httpClient.dispatcher().executorService().shutdownNow();
            } catch (Exception e) {
                LOGGER.debug("Error shutting down shared OkHttpClient", e);
            }
        }
        super.unload();
    }

    @Override
    public boolean canUnload() {
        return true;
    }

    @Override
    public String getDescription() {
        return "AI Traffic Analyst context menu and analysis engine.";
    }
}
