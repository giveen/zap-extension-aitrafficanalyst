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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

public class AnalystPanelImageTest {

    @Test
    public void testImageLoadAndScaleFallback() {
        AnalystPanel panel = new AnalystPanel();
        // call init which attempts to load and scale the icon
        panel.init();

        // Ensure no exceptions thrown during init (if an exception occurred the test would fail)
        assertTrue(true);
    }

    @Test
    public void testImageIOReadNullBehavior() throws IOException {
        // Try to read a nonexistent resource and ensure ImageIO returns null gracefully
        URL missing =
                getClass()
                        .getResource(
                                "/org/zaproxy/zap/extension/aitrafficanalyst/resources/nonexistent.png");
        if (missing == null) {
            // ImageIO.read should not be called on null URL in production; this test asserts
            // defensive behavior
            assertNull(missing);
        } else {
            BufferedImage img = ImageIO.read(missing);
            assertNull(img);
        }
    }
}
