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

public class AnalystPanel extends AbstractPanel {

    private static final long serialVersionUID = 1L;
    private JEditorPane resultArea;
    private StringBuilder fullHistoryMarkdown = new StringBuilder();

    public AnalystPanel() {
        super();
    }

    public void init() {
        this.setLayout(new BorderLayout());
        this.setName("AI Analysis"); // This is the tab name
        // this.setIcon(new ImageIcon(getClass().getResource("/resource/icon.png"))); // TODO: Add icon later

        // Try to load bundled icon from resources and scale it down to a fixed 16x16 BufferedImage
        try {
            java.net.URL iconURL = getClass().getResource("/org/zaproxy/zap/extension/aitrafficanalyst/resources/ai-analysis.png");
            if (iconURL != null) {
                BufferedImage originalImg = ImageIO.read(iconURL);

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
            } else {
                LogManager.getLogger(AnalystPanel.class)
                    .error("AI Analysis Icon NOT FOUND at: /org/zaproxy/zap/extension/aitrafficanalyst/resources/ai-analysis.png");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Toolbar with actions
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton btnClear = new JButton("Clear All");
        btnClear.addActionListener(e -> clearAnalysis());
        toolBar.add(btnClear);
        JButton btnSave = new JButton("Save Report");
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
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public void updateAnalysis(String url, String markdownText) {
        // 1. If this is a "Thinking..." message, show temporary status without appending to history
        if (markdownText != null && markdownText.contains("Thinking...")) {
            String temp = fullHistoryMarkdown.toString() + "\n\n*STATUS: " + markdownText + "*";
            renderHtml(temp);
            return;
        }

        // 2. Permanent append to markdown history
        fullHistoryMarkdown.append("\n\n---\n### Analysis for: ").append(url).append("\n\n");
        fullHistoryMarkdown.append(markdownText);

        // 3. Render the combined markdown
        renderHtml(fullHistoryMarkdown.toString());
    }

    private void renderHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder()
            .softbreak("<br/>")
            .build();
        
        String htmlContent = renderer.render(document);
        
        // Add some basic CSS for a clean "Report" look
        String finalHtml = "<html><body>" + htmlContent + "</body></html>";

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
