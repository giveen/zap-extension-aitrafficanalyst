package org.zaproxy.zap.extension.aitrafficanalyst.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Color;
import javax.swing.UIManager;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import org.apache.logging.log4j.LogManager;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileWriter;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import org.parosproxy.paros.extension.AbstractPanel;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class AnalystPanel extends AbstractPanel {

    private static final long serialVersionUID = 1L;
    private JEditorPane resultArea;
    private StringBuilder fullHistoryMarkdown = new StringBuilder();
        private static final Parser MARKDOWN_PARSER = Parser.builder().build();
        private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder()
            .softbreak("<br/>")
            .escapeHtml(true)
            .build();
    private static final int MAX_HISTORY_CHARS = 2 * 1024 * 1024; // 2 MB
    private static final int KEEP_HISTORY_CHARS = MAX_HISTORY_CHARS / 2; // keep newest 1MB when trimming

    public AnalystPanel() {
        super();
    }

    public void init() {
        this.setLayout(new BorderLayout());
        this.setName(org.parosproxy.paros.Constant.messages.getString("aitrafficanalyst.panel.name")); // This is the tab name
        // this.setIcon(new ImageIcon(getClass().getResource("/resource/icon.png"))); // TODO: Add icon later

        // Try to load bundled icon from resources and scale it down to a fixed 16x16 BufferedImage
        try {
            java.net.URL iconURL = getClass().getResource("/org/zaproxy/zap/extension/aitrafficanalyst/resources/ai-analysis.png");
            if (iconURL != null) {
                BufferedImage originalImg = null;
                try {
                    originalImg = ImageIO.read(iconURL);
                } catch (Exception readEx) {
                    LogManager.getLogger(AnalystPanel.class).warn("ImageIO.read failed, will fallback to ImageIcon", readEx);
                }

                if (originalImg == null) {
                    // Fallback to ImageIcon which may handle different image types
                    try {
                        ImageIcon fallback = new ImageIcon(iconURL);
                        Image scaled = fallback.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                        BufferedImage finalImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2Fallback = finalImg.createGraphics();
                        g2Fallback.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2Fallback.drawImage(scaled, 0, 0, null);
                        g2Fallback.dispose();
                        this.setIcon(new ImageIcon(finalImg));
                    } catch (Exception fbEx) {
                        LogManager.getLogger(AnalystPanel.class).error("Failed to create fallback icon", fbEx);
                    }
                } else {
                    // Prepare final canvas with alpha channel
                    BufferedImage finalImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = finalImg.createGraphics();

                    // Detect Theme Brightness
                    Color bg = UIManager.getColor("Panel.background");
                    int brightness = (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3;
                    boolean isDarkTheme = brightness < 128;

                    // Use high-quality interpolation
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // If dark theme, draw a subtle light glow behind the icon for contrast
                    if (isDarkTheme) {
                        g2.setColor(new Color(255, 255, 255, 40)); // Semi-transparent white
                        g2.fillOval(1, 1, 14, 14);
                    }

                    // Draw icon centered with 1px padding (14x14)
                    g2.drawImage(originalImg, 1, 1, 14, 14, null);
                    g2.dispose();

                    this.setIcon(new ImageIcon(finalImg));
                }
            } else {
                LogManager.getLogger(AnalystPanel.class)
                    .error("AI Analysis Icon NOT FOUND at: /org/zaproxy/zap/extension/aitrafficanalyst/resources/ai-analysis.png");
            }
        } catch (RuntimeException e) {
            LogManager.getLogger(AnalystPanel.class).error("Unexpected error while loading icon", e);
        }

        // Toolbar with actions
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton btnClear = new JButton(org.parosproxy.paros.Constant.messages.getString("aitrafficanalyst.btn.clear"));
        btnClear.addActionListener(e -> clearAnalysis());
        toolBar.add(btnClear);
        JButton btnSave = new JButton(org.parosproxy.paros.Constant.messages.getString("aitrafficanalyst.btn.save"));
        btnSave.addActionListener(e -> saveReport());
        toolBar.add(btnSave);
        this.add(toolBar, BorderLayout.NORTH);

        resultArea = new JEditorPane();
        resultArea.setEditable(false);
        resultArea.setContentType("text/html");

        // Fix: Add CSS rules directly to the EditorKit's StyleSheet
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body { font-family: sans-serif; padding: 15px; line-height: 1.5; }");
        styleSheet.addRule("h3 { color: #2c3e50; border-bottom: 1px solid #eee; padding-bottom: 5px; margin-top: 20px; }");
        styleSheet.addRule("code { background-color: #f8f9fa; padding: 2px 4px; border-radius: 4px; color: #e83e8c; font-family: monospace; }");
        styleSheet.addRule("blockquote { border-left: 5px solid #dfe2e5; color: #6a737d; padding-left: 1em; margin-left: 0; font-style: italic; }");
        styleSheet.addRule("hr { border: 0; border-top: 2px solid #eee; margin: 20px 0; }");
        resultArea.setEditorKit(kit);

        JScrollPane scrollPane = new JScrollPane(resultArea);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    public void clearAnalysis() {
        fullHistoryMarkdown.setLength(0);
        if (resultArea != null) {
            resultArea.setText("");
        }
    }

    private void saveReport() {
        JFileChooser chooser = new JFileChooser();
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(fullHistoryMarkdown.toString());
                JOptionPane.showMessageDialog(this, "Report saved to " + file.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Error saving report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public void updateAnalysis(String url, String markdownText) {
        // 1. If this is a "Thinking..." message, show temporary status without appending to history
        String thinkingKey = org.parosproxy.paros.Constant.messages.getString("aitrafficanalyst.status.thinking");
        if (markdownText != null && markdownText.contains(thinkingKey)) {
            String temp = fullHistoryMarkdown.toString() + "\n\n*STATUS: " + markdownText + "*";
            renderHtml(temp);
            return;
        }

        // 2. Permanent append to markdown history
        String text = markdownText != null ? markdownText : "";
        // Avoid duplicating headers if the model already included an analysis header
        String trimmed = text.trim();
        boolean hasHeader = trimmed.startsWith("### Analysis for:") || trimmed.startsWith("Analysis for:") || trimmed.contains("Analysis for: " + url);
        if (!hasHeader) {
            fullHistoryMarkdown.append("\n\n---\n### Analysis for: ").append(url).append("\n\n");
        }
        fullHistoryMarkdown.append(text);

        // 2.a Prune history if it grows beyond MAX_HISTORY_CHARS
        if (fullHistoryMarkdown.length() > MAX_HISTORY_CHARS) {
            // Keep only the newest KEEP_HISTORY_CHARS characters and insert a truncation marker
            String tail = fullHistoryMarkdown.substring(fullHistoryMarkdown.length() - KEEP_HISTORY_CHARS);
            fullHistoryMarkdown.setLength(0);
            fullHistoryMarkdown.append("\n\n[... previous history truncated due to size ...]\n");
            fullHistoryMarkdown.append(tail);
        }

        // 3. Render the combined markdown
        renderHtml(fullHistoryMarkdown.toString());
    }

    private void renderHtml(String markdown) {
        Node document = MARKDOWN_PARSER.parse(markdown);
        String htmlContent = HTML_RENDERER.render(document);

        // Sanitize HTML using a permissive but safe whitelist (no scripts/styles)
        String cleanHtml = Jsoup.clean(htmlContent, Safelist.basic());

        String finalHtml = "<html><body>" + cleanHtml + "</body></html>";

        javax.swing.SwingUtilities.invokeLater(() -> {
            resultArea.setText(finalHtml);
            resultArea.setCaretPosition(0);
        });
    }

    public void setTabFocus() {
        // Try to give focus to the text area in the panel
        if (resultArea != null) {
            resultArea.requestFocusInWindow();
        }
    }
}
