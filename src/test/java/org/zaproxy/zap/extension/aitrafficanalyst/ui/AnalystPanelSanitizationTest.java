package org.zaproxy.zap.extension.aitrafficanalyst.ui;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AnalystPanelSanitizationTest {

    @Test
    public void testScriptTagIsSanitized() {
        String malicious = "Hello<script>alert('x')</script>World";

        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().softbreak("<br/>").escapeHtml(true).build();
        Node document = parser.parse(malicious);
        String html = renderer.render(document);
        String cleaned = Jsoup.clean(html, Safelist.basic());

        assertFalse(cleaned.contains("<script"), "Sanitized HTML must not contain <script>");
        assertFalse(cleaned.toLowerCase().contains("javascript:"), "Sanitized HTML must not contain javascript: URIs");
    }
}
