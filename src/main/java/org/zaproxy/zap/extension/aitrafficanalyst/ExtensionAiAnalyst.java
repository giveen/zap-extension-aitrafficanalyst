package org.zaproxy.zap.extension.aitrafficanalyst;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import java.time.Duration;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.AnalystPanel;
import javax.swing.ImageIcon;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.AnalystOptionsPanel;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.AnalystPopupMenu;

public class ExtensionAiAnalyst extends ExtensionAdaptor {

    public static final String NAME = "ExtensionAiAnalyst";
    private static final Logger LOGGER = LogManager.getLogger(ExtensionAiAnalyst.class);
    
    private AnalystPanel analystPanel;
    private AnalystOptions options;
    private ExecutorService executor;
    private OkHttpClient httpClient;

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
        this.executor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("AiAnalyst-worker-" + System.nanoTime());
                return t;
            }
        });
        // Shared OkHttpClient for reuse across Ollama calls
        this.httpClient = new OkHttpClient.Builder()
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
            this.analystPanel = new AnalystPanel();
            this.analystPanel.init();
            try {
                ImageIcon icon = new ImageIcon(getClass().getResource("/org/zaproxy/zap/extension/aitrafficanalyst/resources/robot.png"));
                this.analystPanel.setIcon(icon);
            } catch (RuntimeException e) {
                LOGGER.debug("Analyst icon failed to load (runtime issue)", e);
            }
            extensionHook.getHookView().addStatusPanel(this.analystPanel);
            
            // Register Options Panel
            AnalystOptionsPanel optionsPanel = new AnalystOptionsPanel(this);
            optionsPanel.init();
            extensionHook.getHookView().addOptionPanel(optionsPanel);
            
            // Register Popup Menu
            extensionHook.getHookMenu().addPopupMenuItem(new AnalystPopupMenu(this));
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
