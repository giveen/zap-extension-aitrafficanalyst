package org.zaproxy.zap.extension.aitrafficanalyst;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.AnalystPanel;
import javax.swing.ImageIcon;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.AnalystOptionsPanel;
import org.zaproxy.zap.extension.aitrafficanalyst.ui.AnalystPopupMenu;

public class ExtensionAiAnalyst extends ExtensionAdaptor {

    public static final String NAME = "ExtensionAiAnalyst";
    private static final Logger LOGGER = LogManager.getLogger(ExtensionAiAnalyst.class);
    
    private AnalystPanel analystPanel;
    private AnalystOptions options;

    public ExtensionAiAnalyst() {
        super(NAME);
    }

    @Override
    public void init() {
        super.init();
        setI18nPrefix("aitrafficanalyst");
        // Register our options class with ZAP's configuration system
        this.options = new AnalystOptions();
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
            } catch (Exception e) {
                LOGGER.debug("Analyst icon not found or failed to load", e);
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

    @Override
    public boolean canUnload() {
        return true;
    }

    @Override
    public String getDescription() {
        return "AI Traffic Analyst context menu and analysis engine.";
    }
}
