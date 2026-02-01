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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.parosproxy.paros.common.AbstractParam;

public class AnalystOptions extends AbstractParam {

    // Phase 2: legacy Ollama settings are no longer used.
    @Deprecated private static final String PARAM_OLLAMA_URL = "aitrafficanalyst.ollama.url";
    @Deprecated private static final String PARAM_MODEL_NAME = "aitrafficanalyst.ollama.model";
    // Backward compatibility for earlier builds that stored a single prompt.
    @Deprecated private static final String PARAM_SYSTEM_PROMPT = "aitrafficanalyst.ollama.prompt";

    private static final String PARAM_ROLES_B64 = "aitrafficanalyst.roles.b64";

    // Legacy storage format (JSON map) used by earlier builds.
    private static final String PARAM_ROLES_JSON = "aitrafficanalyst.roles.json";
    private static final String PARAM_ACTIVE_ROLE = "aitrafficanalyst.activeRole";
    // Backward compatibility for earlier 1.1.0 builds.
    private static final String PARAM_ACTIVE_ROLE_LEGACY = "aitrafficanalyst.roles.active";

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
    private Map<String, String> roles = new LinkedHashMap<>();
    private String activeRole = DEFAULT_ROLE;

    @Override
    protected void parse() {
        // Load roles using the current storage format, with migration from the legacy JSON format.
        Map<String, String> parsedRoles = null;
        String rolesB64 = getString(PARAM_ROLES_B64, null);
        if (rolesB64 != null && !rolesB64.trim().isEmpty()) {
            parsedRoles = parseRolesB64(rolesB64);
        }

        boolean migratedFromLegacyJson = false;
        if (parsedRoles == null || parsedRoles.isEmpty()) {
            String rolesJson = getString(PARAM_ROLES_JSON, null);
            if (rolesJson != null && !rolesJson.trim().isEmpty()) {
                parsedRoles = parseRolesJson(rolesJson);
                migratedFromLegacyJson = parsedRoles != null && !parsedRoles.isEmpty();
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

        // Phase 2 cleanup: remove legacy Ollama settings if present.
        try {
            getConfig().clearProperty(PARAM_OLLAMA_URL);
            getConfig().clearProperty(PARAM_MODEL_NAME);
        } catch (Exception e) {
            // Ignore configuration implementations that do not support clearProperty.
        }

        // If we loaded legacy JSON roles, migrate them to the current storage format.
        if (migratedFromLegacyJson) {
            persistRoles();
        }
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
        String b64 = encodeRolesB64(roles);
        getConfig().setProperty(PARAM_ROLES_B64, b64);
        try {
            // Remove legacy JSON storage to avoid dead/confusing config.
            getConfig().clearProperty(PARAM_ROLES_JSON);
        } catch (Exception e) {
            // Ignore.
        }
    }

    private static String encodeRolesB64(Map<String, String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }
        Base64.Encoder enc = Base64.getEncoder();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : roles.entrySet()) {
            String key = e.getKey() == null ? "" : e.getKey();
            String value = e.getValue() == null ? "" : e.getValue();
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(enc.encodeToString(key.getBytes(StandardCharsets.UTF_8)))
                    .append('=')
                    .append(enc.encodeToString(value.getBytes(StandardCharsets.UTF_8)));
        }
        return sb.toString();
    }

    private static Map<String, String> parseRolesB64(String rolesB64) {
        if (rolesB64 == null) {
            return null;
        }
        String text = rolesB64.trim();
        if (text.isEmpty()) {
            return null;
        }
        Base64.Decoder dec = Base64.getDecoder();
        Map<String, String> out = new LinkedHashMap<>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String l = line.trim();
            if (l.isEmpty()) {
                continue;
            }
            int idx = l.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            try {
                String kB64 = l.substring(0, idx);
                String vB64 = l.substring(idx + 1);
                String k = new String(dec.decode(kB64), StandardCharsets.UTF_8);
                String v = new String(dec.decode(vB64), StandardCharsets.UTF_8);
                if (!k.trim().isEmpty()) {
                    out.put(k, v);
                }
            } catch (IllegalArgumentException e) {
                // Ignore malformed lines.
            }
        }
        return out.isEmpty() ? null : out;
    }

    private static Map<String, String> parseRolesJson(String rolesJson) {
        if (rolesJson == null) {
            return null;
        }
        String s = rolesJson.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            int[] idx = new int[] {0};
            skipWs(s, idx);
            if (idx[0] >= s.length() || s.charAt(idx[0]) != '{') {
                return null;
            }
            idx[0]++; // {
            Map<String, String> out = new LinkedHashMap<>();
            while (true) {
                skipWs(s, idx);
                if (idx[0] >= s.length()) {
                    return null;
                }
                char c = s.charAt(idx[0]);
                if (c == '}') {
                    idx[0]++;
                    break;
                }
                String key = parseJsonString(s, idx);
                if (key == null) {
                    return null;
                }
                skipWs(s, idx);
                if (idx[0] >= s.length() || s.charAt(idx[0]) != ':') {
                    return null;
                }
                idx[0]++;
                skipWs(s, idx);
                String value = parseJsonString(s, idx);
                if (value == null) {
                    return null;
                }
                if (!key.trim().isEmpty()) {
                    out.put(key, value);
                }
                skipWs(s, idx);
                if (idx[0] >= s.length()) {
                    return null;
                }
                char sep = s.charAt(idx[0]);
                if (sep == ',') {
                    idx[0]++;
                    continue;
                }
                if (sep == '}') {
                    idx[0]++;
                    break;
                }
                return null;
            }
            return out.isEmpty() ? null : out;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void skipWs(String s, int[] idx) {
        while (idx[0] < s.length()) {
            char c = s.charAt(idx[0]);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                idx[0]++;
            } else {
                return;
            }
        }
    }

    private static String parseJsonString(String s, int[] idx) {
        if (idx[0] >= s.length() || s.charAt(idx[0]) != '"') {
            return null;
        }
        idx[0]++; // opening quote
        StringBuilder sb = new StringBuilder();
        while (idx[0] < s.length()) {
            char c = s.charAt(idx[0]++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (idx[0] >= s.length()) {
                return null;
            }
            char esc = s.charAt(idx[0]++);
            switch (esc) {
                case '"':
                    sb.append('"');
                    break;
                case '\\':
                    sb.append('\\');
                    break;
                case '/':
                    sb.append('/');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'u':
                    if (idx[0] + 4 > s.length()) {
                        return null;
                    }
                    String hex = s.substring(idx[0], idx[0] + 4);
                    idx[0] += 4;
                    try {
                        int code = Integer.parseInt(hex, 16);
                        sb.append((char) code);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                    break;
                default:
                    return null;
            }
        }
        return null;
    }

    public void resetToDefaults() {
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
