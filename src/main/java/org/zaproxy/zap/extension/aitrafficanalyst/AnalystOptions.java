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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.parosproxy.paros.common.AbstractParam;

public class AnalystOptions extends AbstractParam {

    private static final String PARAM_OLLAMA_URL = "aitrafficanalyst.ollama.url";
    private static final String PARAM_MODEL_NAME = "aitrafficanalyst.ollama.model";
    private static final String PARAM_SYSTEM_PROMPT = "aitrafficanalyst.ollama.prompt";

    private static final String PARAM_ROLES_JSON = "aitrafficanalyst.roles.json";
    private static final String PARAM_ACTIVE_ROLE = "aitrafficanalyst.activeRole";
    // Backward compatibility for earlier 1.1.0 builds.
    private static final String PARAM_ACTIVE_ROLE_LEGACY = "aitrafficanalyst.roles.active";

    // Defaults
    private String ollamaUrl = "http://localhost:11434/";
    private String modelName = "llama3:70b";

    // Default persona names
    private static final String ROLE_STANDARD = "Standard Analyst";
    private static final String ROLE_RED_TEAM = "Red Teamer (Offensive)";
    private static final String ROLE_SKEPTIC = "False Positive Hunter";
    private static final String ROLE_API = "API Logic Expert";

    public static final String DEFAULT_ROLE = ROLE_STANDARD;

    private static final String PROMPT_STANDARD =
            "You are a battle-hardened web security researcher dissecting any HTTP/HTTPS traffic—single request/response, HAR files, WebSocket streams, traffic sequences, or full session captures.\n"
                    + "\n"
                    + "### Adapt your analysis to the input format ###\n"
                    + "- Single Request/Response → Static analysis\n"
                    + "- HAR/JSON → Multi-message flows, timing attacks, state changes\n"
                    + "- WebSocket → Framing issues, auth token leaks, oversized payloads\n"
                    + "- Sequence → Business logic flaws, CSRF chains, session fixation\n"
                    + "\n"
                    + "### Hunt across THIS messages for OWASP Top 10 + advanced issues ###\n"
                    + "- Access: IDOR, BOLA, missing authz per message\n"
                    + "- Secrets: Creds/tokens/PII in ANY field (URL, headers, body, WS frames)\n"
                    + "- Injection: Reflections, errors, payloads across request→response chains\n"
                    + "- Headers: Per-message security headers, CORS evolution, HSTS preload issues\n"
                    + "- Auth: Session handling across flows, token rotation, logout bypass\n"
                    + "- Logic: State manipulation, race conditions, parameter pollution\n"
                    + "- Supply Chain: Version disclosure, deserialization across messages\n"
                    + "- Network: SSRF, open redirects, DNS rebinding hints\n"
                    + "\n"
                    + "### Output exactly 3 high-impact hypotheses, ranked by exploitability ###\n"
                    + "- 1. HYPOTHESIS: [Specific attack across messages/flows]\n"
                    + "   EVIDENCE: [Message #/timestamp + direct quotes proving it]\n"
                    + "   RISK: [CVSS + exploit path]\n"
                    + "   TEST: [ZAProxy scan/passive rule/active scan/traffic replay]\n"
                    + "\n"
                    + "- 2. [Same format]\n"
                    + "\n"
                    + "- 3. [Same format]\n";

    private static final String PROMPT_RED_TEAM =
            "You are a blackhat-turned-RedTeam operator. Goal: EXPLOITATION. No theory, no CVSS scores, just payloads that work.\n"
                    + "\n"
                    + "Given this HTTP traffic, weaponize the FIRST realistic attack path\n"
                    + "\n"
                    + "- 1. PRIMARY VECTOR: [Most direct exploit - XSS→XSS, SQLi→dump, IDOR→pivot, etc.]\n"
                    + "- 2. PAYLOAD: [Copy-paste curl command OR 10-line Python requests]\n"
                    + "- 3. IMPACT: [What you get immediately - shell, data dump, priv esc, lateral movement]\n"
                    + "- 4. CHAIN: [Next logical step after success - \"now pivot to admin panel\"]\n"
                    + "- 5. EVADE: [Bypass WAF/IPS if obvious - UA spoof, encoding, timing]\n"
                    + "\n"
                    + "Format - DEAD SIMPLE\n"
                    + "```\n"
                    + "$ curl -X POST \"https://target.com/vuln\" \\\n"
                    + "  -d \"username=admin' OR 1=1--\" \\\n"
                    + "  -H \"X-Forwarded-For: 127.0.0.1\" \\\n"
                    + "  -v\n"
                    + "\n"
                    + "IMPACT: Full DB dump, 500k user records\n"
                    + "CHAIN: Extract API keys → target.internal.s3.aws.com\n"
                    + "```\n"
                    + "\n"
                    + "Rules:\n"
                    + "\n"
                    + "    Real payloads only (no \"alert(1)\")\n"
                    + "    Adapt to traffic type (WS → malformed frames, HAR → replay attacks)\n"
                    + "    Fail closed: \"NO EXPLOIT PATH\" if truly clean\n"
                    + "    Burp/ZAP ready (copy-paste to Repeater/Intruder)\n";

    private static final String PROMPT_SKEPTIC =
            "You are a grizzled pentester who lives to kill false positives. Scanner alerts are guilty until proven innocent.\n"
                    + "\n"
                    + "Given this traffic + [scanner claim: SQLi/XSS/CSRF/etc.], DISMANTLE it:\n"
                    + "\n"
                    + "EXAMPLE CLAIM: [Scanner said \"XSS in param 'search'\"]\n"
                    + "\n"
                    + "FALSE POSITIVE EVIDENCE:\n"
                    + "✓ NO reflection: \"search=foo\" → response contains \"search=test&q=foo\" (URL encoded, no parse)\n"
                    + "✓ WAF blocked: Status 403 + \"blocked by ModSecurity\"\n"
                    + "✓ Safe context: Inside <script>document.title=\"foo\"</script> (HTML attribute)\n"
                    + "✓ Error handling: Generic 500, no DB error strings/timing anomalies\n"
                    + "✓ Content-Length unchanged: 1423 bytes baseline vs 1425 bytes payload\n"
                    + "\n"
                    + "VERDICT: [FALSE POSITIVE | NEEDS MANUAL CONFIRMATION | LEGIT]\n"
                    + "MANUAL TEST: curl -d \"search=foo\"><script>alert(1)</script>\" target.com\n"
                    + "EXPECTED: Same safe response pattern\n"
                    + "\n"
                    + "Kill criteria (in order):\n"
                    + "\n"
                    + "    No reflection/execution context\n"
                    + "    Generic errors (no DB/server fingerprints)\n"
                    + "    WAF/IPS signatures (403s, custom blocks)\n"
                    + "    Timing/content-length identical across payloads\n"
                    + "    Safe encodings (URL, HTML, JS string context)\n"
                    + "\n"
                    + "Multi-message/HAR: Track state changes—legit vulns escalate, FPs stay static.\n"
                    + "\n"
                    + "Output only when >80% confident it's noise. Otherwise: \"Manual verification required.\"\n";

    private static final String PROMPT_API =
            "You are an API assassin who finds million-dollar logic breaks scanners miss. Ignore OWASP Top 10 noise. Hunt BUSINESS LOGIC + AUTHZ exclusively.\n"
                    + "\n"
                    + "EXAMPLE:\n"
                    + "Dissect this API traffic for:\n"
                    + "\n"
                    + "- IDOR/BOLA: ID=123 → ID=124? Negative IDs? UUID fuzzing? Parent/child resource swaps?\n"
                    + "- MASS ASSIGNMENT: Extra JSON fields (admin=true, role=admin, credits=999999)?\n"
                    + "- FUNCTION LEVEL: User hitting /admin/*? /reports? /pricing? Parameter-based auth bypass?\n"
                    + "- TOXIC DATA: Negative quantities, future dates, maxint values, enum fuzzing\n"
                    + "- RACE CONDITIONS: Identical requests <100ms apart → double spend/inventory drain?\n"
                    + "- STATE MANIP: Cart→wishlist→cart? Status=pending→shipped? Temp auth tokens?\n"
                    + "- DISCLOSURE: JSON structure reveals user_count, total_revenue, internal_ids?\n"
                    + "\n"
                    + "Output ONLY exploitable flaws:\n"
                    + "- 1. VULN: [IDOR on /api/user/123 → /api/user/124 dumps rival data]\n"
                    + "   PROOF: 200 OK + {\"email\":\"ceo@company.com\",\"salary\":250000}\n"
                    + "   IMPACT: Full customer DB → $500k GDPR fine\n"
                    + "   \n"
                    + "- 2. VULN: [Mass assignment: {\"plan\":\"free\",\"trial_days\":99999}]\n"
                    + "   PROOF: Accepted unlisted field → premium features unlocked\n"
                    + "   EXPLOIT: curl -X PATCH /api/subscription/123 -d '{\"trial_days\":99999}'\n"
                    + "   \n"
                    + "   \n"
                    + "Test every claim:\n"
                    + "- Show before/after request diffs\n"
                    + "- Exact curl reproducing the break\n"
                    + "- Realistic business impact ($/compliance/reputation)\n"
                    + "\n"
                    + "No logic issues? \"API logic secure.\"\n";

    private static final String DEFAULT_PROMPT = PROMPT_STANDARD;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, String> roles = new LinkedHashMap<>();
    private String activeRole = DEFAULT_ROLE;

    @Override
    protected void parse() {
        // Load from ZAP config file
        ollamaUrl = getString(PARAM_OLLAMA_URL, ollamaUrl);
        modelName = getString(PARAM_MODEL_NAME, modelName);

        String rolesJson = getString(PARAM_ROLES_JSON, null);
        Map<String, String> parsedRoles = null;
        if (rolesJson != null && !rolesJson.trim().isEmpty()) {
            try {
                parsedRoles =
                        objectMapper.readValue(
                                rolesJson, new TypeReference<LinkedHashMap<String, String>>() {});
            } catch (Exception e) {
                parsedRoles = null;
            }
        }

        boolean loadedLegacyPrompt = false;
        if (parsedRoles != null && !parsedRoles.isEmpty()) {
            roles = new LinkedHashMap<>(parsedRoles);
        } else {
            roles = new LinkedHashMap<>();
            // Backward compatibility: if a legacy single prompt exists, load it into the default
            // role.
            String legacyPrompt = getString(PARAM_SYSTEM_PROMPT, null);
            if (legacyPrompt != null && !legacyPrompt.trim().isEmpty()) {
                roles.put(DEFAULT_ROLE, legacyPrompt);
                loadedLegacyPrompt = true;
            }
        }

        // Parse the Active Role (new key, then fallback to legacy).
        String configuredActiveRole = getString(PARAM_ACTIVE_ROLE, null);
        if (configuredActiveRole == null || configuredActiveRole.trim().isEmpty()) {
            configuredActiveRole = getString(PARAM_ACTIVE_ROLE_LEGACY, DEFAULT_ROLE);
        }
        activeRole = configuredActiveRole;

        // If upgrading from a legacy single-prompt config, populate the rest of the built-in
        // personas (without overwriting the user's prompt).
        if (loadedLegacyPrompt) {
            if (!roles.containsKey(ROLE_RED_TEAM)) {
                roles.put(ROLE_RED_TEAM, PROMPT_RED_TEAM);
            }
            if (!roles.containsKey(ROLE_SKEPTIC)) {
                roles.put(ROLE_SKEPTIC, PROMPT_SKEPTIC);
            }
            if (!roles.containsKey(ROLE_API)) {
                roles.put(ROLE_API, PROMPT_API);
            }
        }

        ensureDefaults();

        // Safety check: if stored role name doesn't exist (e.g. deleted), revert to default.
        if (!roles.containsKey(activeRole)) {
            activeRole = DEFAULT_ROLE;
        }

        // Persist normalized selection so it survives restarts.
        getConfig().setProperty(PARAM_ACTIVE_ROLE, activeRole);
        getConfig().setProperty(PARAM_ACTIVE_ROLE_LEGACY, activeRole);
    }

    /**
     * Legacy accessor kept for compatibility. Prefer {@link #getRolePrompt(String)} with {@link
     * #getActiveRole()}.
     */
    @Deprecated
    public String getSystemPrompt() {
        return getRolePrompt(getActiveRole());
    }

    /**
     * Legacy mutator kept for compatibility. Sets the prompt for {@link #DEFAULT_ROLE} and makes it
     * the active role.
     */
    @Deprecated
    public void setSystemPrompt(String systemPrompt) {
        setRolePrompt(DEFAULT_ROLE, systemPrompt);
        setActiveRole(DEFAULT_ROLE);
        // Also write the legacy key for older builds.
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

    public String getActiveRole() {
        ensureDefaults();
        return activeRole;
    }

    public void setActiveRole(String activeRole) {
        String roleName = activeRole == null ? "" : activeRole.trim();
        if (roleName.isEmpty() || !roles.containsKey(roleName)) {
            this.activeRole = DEFAULT_ROLE;
        } else {
            this.activeRole = roleName;
        }
        ensureDefaults();
        // Ensure it gets written to zap.xml when ZAP saves config.
        getConfig().setProperty(PARAM_ACTIVE_ROLE, this.activeRole);
        getConfig().setProperty(PARAM_ACTIVE_ROLE_LEGACY, this.activeRole);
    }

    public Map<String, String> getRoles() {
        ensureDefaults();
        return new LinkedHashMap<>(roles);
    }

    public void setRoles(Map<String, String> roles) {
        if (roles == null || roles.isEmpty()) {
            this.roles = new LinkedHashMap<>();
        } else {
            this.roles = new LinkedHashMap<>(roles);
        }
        ensureDefaults();
        persistRoles();
    }

    public String getRolePrompt(String role) {
        ensureDefaults();
        String roleName = (role == null || role.trim().isEmpty()) ? DEFAULT_ROLE : role.trim();
        String prompt = roles.get(roleName);
        if (prompt != null && !prompt.trim().isEmpty()) {
            return prompt;
        }
        prompt = roles.get(DEFAULT_ROLE);
        return (prompt != null && !prompt.trim().isEmpty()) ? prompt : DEFAULT_PROMPT;
    }

    public void setRolePrompt(String role, String prompt) {
        String roleName = (role == null || role.trim().isEmpty()) ? DEFAULT_ROLE : role.trim();
        String value = prompt == null ? "" : prompt;
        roles.put(roleName, value);
        ensureDefaults();
        persistRoles();
        if (DEFAULT_ROLE.equals(roleName)) {
            // Keep legacy key loosely in sync.
            getConfig().setProperty(PARAM_SYSTEM_PROMPT, value);
        }
    }

    public void removeRole(String role) {
        if (role == null) {
            return;
        }
        String roleName = role.trim();
        if (roleName.isEmpty() || DEFAULT_ROLE.equals(roleName)) {
            return;
        }
        roles.remove(roleName);
        ensureDefaults();
        if (!roles.containsKey(activeRole)) {
            activeRole = DEFAULT_ROLE;
            getConfig().setProperty(PARAM_ACTIVE_ROLE, activeRole);
        }
        persistRoles();
    }

    private void ensureDefaults() {
        if (roles == null) {
            roles = new LinkedHashMap<>();
        }
        if (roles.isEmpty()) {
            // Populate all built-in personas on first run.
            roles.put(ROLE_STANDARD, PROMPT_STANDARD);
            roles.put(ROLE_RED_TEAM, PROMPT_RED_TEAM);
            roles.put(ROLE_SKEPTIC, PROMPT_SKEPTIC);
            roles.put(ROLE_API, PROMPT_API);
            activeRole = ROLE_STANDARD;
        }
        if (!roles.containsKey(DEFAULT_ROLE)) {
            roles.put(DEFAULT_ROLE, DEFAULT_PROMPT);
        }
        if (activeRole == null || activeRole.trim().isEmpty() || !roles.containsKey(activeRole)) {
            activeRole = DEFAULT_ROLE;
        }
    }

    private void persistRoles() {
        try {
            String json = objectMapper.writeValueAsString(roles);
            getConfig().setProperty(PARAM_ROLES_JSON, json);
        } catch (Exception e) {
            // If serialization fails, fall back to keeping the roles in memory.
        }
    }

    public void resetToDefaults() {
        this.setOllamaUrl("http://localhost:11434/");
        this.setModelName("llama3:70b");
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put(ROLE_STANDARD, PROMPT_STANDARD);
        defaults.put(ROLE_RED_TEAM, PROMPT_RED_TEAM);
        defaults.put(ROLE_SKEPTIC, PROMPT_SKEPTIC);
        defaults.put(ROLE_API, PROMPT_API);
        this.setRoles(defaults);
        this.setActiveRole(ROLE_STANDARD);
        // Also reset legacy key.
        getConfig().setProperty(PARAM_SYSTEM_PROMPT, PROMPT_STANDARD);
    }
}
