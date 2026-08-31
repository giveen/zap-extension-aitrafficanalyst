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
package org.zaproxy.zap.extension.aitrafficanalyst.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionLoader;
import org.parosproxy.paros.model.Model;

/**
 * LlmAddonClient talks to the real ZAP LLM add-on purely via reflection (no compile-time dependency
 * exists for it), so it can be exercised here with a plain test double registered under an {@code
 * ExtensionLoader} obtained via {@link Control#initSingletonForTesting}.
 *
 * <p>Note: ZAP 2.15.0 (our declared minimum) exposes {@code initSingletonForTesting} but not the
 * later-added {@code setSingletonForTesting}, so there's no supported way to reset the {@code
 * Control} singleton back to a literal {@code null} once set — the "Control singleton not yet
 * initialized" defensive branch in {@code LlmAddonClient.getExtensionLlm()} is exercised only by
 * inspection, not a dedicated test here.
 */
public class LlmAddonClientTest {

    private final LlmAddonClient client = new LlmAddonClient();

    @Test
    void notConfiguredWhenLlmExtensionNotLoaded() {
        ExtensionLoader loader = new ExtensionLoader(Model.getSingleton(), null);
        Control.initSingletonForTesting(Model.getSingleton(), loader);

        assertFalse(client.isConfigured());
    }

    @Test
    void delegatesChatAndEvictsCommsServiceAfterward() throws Exception {
        FakeCommsService comms = new FakeCommsService();
        comms.responseToReturn = "analysis result";
        FakeLlmExtension fake = new FakeLlmExtension();
        fake.configured = true;
        fake.modelName = "claude-sonnet-5";
        fake.commsService = comms;

        ExtensionLoader loader = new ExtensionLoader(Model.getSingleton(), null);
        loader.addExtension(fake);
        Control.initSingletonForTesting(Model.getSingleton(), loader);

        assertTrue(client.isConfigured());
        assertEquals("claude-sonnet-5", client.getModelName());

        String result = client.chat("analyze this request");

        assertEquals("analysis result", result);
        assertEquals("analyze this request", comms.lastPrompt);
        // The cached communication service must be evicted after every call so its rolling
        // chat memory doesn't accumulate across unrelated analyses (see LlmAddonClient.chat).
        assertEquals(List.of("aitrafficanalyst"), fake.removedCommsKeys);
    }

    /** Stands in for org.zaproxy.addon.llm.ExtensionLlm; matched by name via reflection. */
    static class FakeLlmExtension extends ExtensionAdaptor {
        boolean configured;
        String modelName = "";
        Object commsService;
        final List<String> removedCommsKeys = new ArrayList<>();

        FakeLlmExtension() {
            super("ExtensionLlm");
        }

        public boolean isConfigured() {
            return configured;
        }

        public String getCommsIssue() {
            return "";
        }

        public String getDefaultModelName() {
            return modelName;
        }

        public Object getCommunicationService(String commsKey, String outputTabName) {
            return commsService;
        }

        public void removeCommunicationService(String commsKey) {
            removedCommsKeys.add(commsKey);
        }
    }

    /** Stands in for org.zaproxy.addon.llm.services.LlmCommunicationService. */
    static class FakeCommsService {
        String lastPrompt;
        String responseToReturn;

        public String chat(String prompt) {
            lastPrompt = prompt;
            return responseToReturn;
        }
    }
}
