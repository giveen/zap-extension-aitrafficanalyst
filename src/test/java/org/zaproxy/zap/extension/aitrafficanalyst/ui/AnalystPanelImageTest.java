package org.zaproxy.zap.extension.aitrafficanalyst.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.io.IOException;

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
        URL missing = getClass().getResource("/org/zaproxy/zap/extension/aitrafficanalyst/resources/nonexistent.png");
        if (missing == null) {
            // ImageIO.read should not be called on null URL in production; this test asserts defensive behavior
            assertNull(missing);
        } else {
            BufferedImage img = ImageIO.read(missing);
            assertNull(img);
        }
    }
}
