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

import static org.junit.jupiter.api.Assertions.*;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

public class AnalystPanelSanitizationTest {

    private static String renderAndClean(String markdown) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().softbreak("<br/>").escapeHtml(true).build();
        Node document = parser.parse(markdown);
        String html = renderer.render(document);
        // Exercise AnalystPanel's actual sanitizer (not a hand-copied Safelist.basic()) so this
        // test tracks whatever renderHtml really uses.
        return Jsoup.clean(html, AnalystPanel.ANALYSIS_SAFELIST);
    }

    @Test
    public void testScriptTagIsSanitized() {
        String cleaned = renderAndClean("Hello<script>alert('x')</script>World");

        assertFalse(cleaned.contains("<script"), "Sanitized HTML must not contain <script>");
        assertFalse(
                cleaned.toLowerCase().contains("javascript:"),
                "Sanitized HTML must not contain javascript: URIs");
    }

    @Test
    public void testHeadingsAndHorizontalRulesSurviveSanitization() {
        // Regression test: Safelist.basic() alone strips <h1-6> and <hr> entirely, which used to
        // silently discard the "### Analysis for: <url>" / "---" separators AnalystPanel injects
        // between history entries, plus any headings in the model's own Markdown output.
        String cleaned = renderAndClean("### Analysis for: http://example.com\n\n---\n\nBody text");

        assertTrue(cleaned.contains("<h3>"), "Headings must survive sanitization");
        assertTrue(cleaned.contains("<hr"), "Horizontal rules must survive sanitization");
    }
}
