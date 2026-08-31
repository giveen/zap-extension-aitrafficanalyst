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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Covers the legacy JSON role-storage migration path, which used to be a hand-rolled recursive
 * descent parser and now delegates to json-lib.
 */
public class AnalystOptionsTest {

    @Test
    public void parsesFlatStringMap() {
        String json = "{\"Standard Analyst\":\"line1\",\"Red Teamer\":\"line2\"}";

        Map<String, String> roles = AnalystOptions.parseRolesJson(json);

        assertEquals(Map.of("Standard Analyst", "line1", "Red Teamer", "line2"), roles);
    }

    @Test
    public void handlesEscapesAndUnicode() {
        String json = "{\"role\":\"line1\\nline2 with \\\"quotes\\\" and \\u00e9\"}";

        Map<String, String> roles = AnalystOptions.parseRolesJson(json);

        assertEquals("line1\nline2 with \"quotes\" and \u00e9", roles.get("role"));
    }

    @Test
    public void returnsNullForBlankOrNullInput() {
        assertNull(AnalystOptions.parseRolesJson(null));
        assertNull(AnalystOptions.parseRolesJson(""));
        assertNull(AnalystOptions.parseRolesJson("   "));
    }

    @Test
    public void returnsNullForMalformedJson() {
        assertNull(AnalystOptions.parseRolesJson("{not json"));
    }

    @Test
    public void returnsNullForEmptyObject() {
        assertNull(AnalystOptions.parseRolesJson("{}"));
    }

    @Test
    public void skipsBlankKeys() {
        String json = "{\"\":\"ignored\",\"role\":\"kept\"}";

        Map<String, String> roles = AnalystOptions.parseRolesJson(json);

        assertEquals(Map.of("role", "kept"), roles);
    }
}
