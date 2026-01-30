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
package org.zaproxy.zap.extension.aitrafficanalyst;

import org.parosproxy.paros.common.AbstractParam;

public class AnalystOptions extends AbstractParam {

    private static final String PARAM_OLLAMA_URL = "aitrafficanalyst.ollama.url";
    private static final String PARAM_MODEL_NAME = "aitrafficanalyst.ollama.model";
    private static final String PARAM_SYSTEM_PROMPT = "aitrafficanalyst.ollama.prompt";

    // Defaults
    private String ollamaUrl = "http://localhost:11434/";
    private String modelName = "llama3:70b";
    private static final String DEFAULT_PROMPT =
            "You are a world-class Web Security Expert and Researcher.\n"
                    + "Analyze the provided HTTP Request/Response interaction to identify potential vulnerabilities focusing on the OWASP Top 10 (2021-2026) framework.\n\n"
                    + "Focus areas:\n"
                    + "1. Broken Access Control: Does the Response reveal data belonging to other users or bypass restricted paths?\n"
                    + "2. Cryptographic Failures: Are passwords or PII visible in the Request OR leaked in the Response?\n"
                    + "3. Injection: Check parameters in the Request and see if they are reflected or cause errors in the Response.\n"
                    + "4. Insecure Design: Evaluate the workflow logic for flaws; is the server trusting client-side state blindly?\n"
                    + "5. Security Misconfiguration: Analyze Response headers for missing security controls (CSP, HSTS, X-Content-Type-Options).\n"
                    + "6. Vulnerable Components: Check the 'Server' or 'X-Powered-By' headers for outdated software versions.\n"
                    + "7. Identification/Authentication: Does the Response set insecure cookies or reveal session details?\n"
                    + "8. Integrity Failures: Look for serialized objects in the Request/Response.\n"
                    + "9. Logging/Monitoring: Note if sensitive actions lack confirmation or unique identifiers.\n"
                    + "10. SSRF: If the Request has a URL parameter, does the Response indicate an internal fetch occurred?\n\n"
                    + "Provide 3 high-impact security hypotheses. Explicitly state if the Response body or headers provide evidence to support your findings.";
    private String systemPrompt = DEFAULT_PROMPT;

    @Override
    protected void parse() {
        // Load from ZAP config file
        ollamaUrl = getString(PARAM_OLLAMA_URL, ollamaUrl);
        modelName = getString(PARAM_MODEL_NAME, modelName);
        systemPrompt = getString(PARAM_SYSTEM_PROMPT, systemPrompt);
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        getConfig().setProperty(PARAM_SYSTEM_PROMPT, systemPrompt);
    }

    public String getOllamaUrl() {
        return ollamaUrl;
    }

    public void setOllamaUrl(String ollamaUrl) {
        this.ollamaUrl = ollamaUrl;
        getConfig().setProperty(PARAM_OLLAMA_URL, ollamaUrl);
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
        getConfig().setProperty(PARAM_MODEL_NAME, modelName);
    }

    public void resetToDefaults() {
        this.setOllamaUrl("http://localhost:11434/");
        this.setModelName("llama3:70b");
        this.setSystemPrompt(DEFAULT_PROMPT);
    }
}
