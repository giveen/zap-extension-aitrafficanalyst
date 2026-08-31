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

/**
 * Internal abstraction over the LLM implementation used by this add-on.
 *
 * <p>Phase 1 uses the official ZAP LLM add-on via reflection, to avoid requiring a compile-time
 * dependency on the llm add-on artifact.
 */
public interface AnalystLlmClient {

    /**
     * @return {@code true} if the LLM add-on is present and configured.
     */
    boolean isConfigured();

    /**
     * @return a human-friendly issue description if unconfigured, otherwise an empty string.
     */
    String getCommsIssue();

    /**
     * @return the currently configured default model name, or empty string if unknown.
     */
    String getModelName();

    /**
     * Sends a single prompt to the configured model.
     *
     * @param prompt the full prompt to send.
     * @return the response text.
     * @throws Exception if the request fails.
     */
    String chat(String prompt) throws Exception;
}
