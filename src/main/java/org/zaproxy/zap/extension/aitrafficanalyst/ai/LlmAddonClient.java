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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.ExtensionLoader;

/**
 * Uses the official ZAP LLM add-on (ExtensionLlm) via reflection.
 *
 * <p>This project is a standalone add-on and does not have a published Maven dependency for the LLM
 * add-on, so we avoid compile-time linking.
 */
public class LlmAddonClient implements AnalystLlmClient {

    private static final String EXTENSION_LLM_NAME = "ExtensionLlm";
    private static final String EXTENSION_LLM_CLASSNAME_SUFFIX = ".ExtensionLlm";
    private static final String COMMS_KEY = "aitrafficanalyst";
    private static final String OUTPUT_TAB_NAME = "AI Traffic Analyst";

    private volatile Extension extensionLlm;

    private Extension getExtensionLlm() {
        Extension cached = extensionLlm;
        if (cached != null) {
            return cached;
        }

        // Control.getSingleton() is a plain static field, null until ZAP initializes it (e.g.
        // when called from a unit test, or theoretically very early in startup), so it can't be
        // dereferenced directly.
        Control control = Control.getSingleton();
        ExtensionLoader loader = control != null ? control.getExtensionLoader() : null;
        Extension ext = loader != null ? loader.getExtension(EXTENSION_LLM_NAME) : null;

        // Try alternate keys (some APIs use class name).
        if (ext == null && loader != null) {
            ext = loader.getExtension("org.zaproxy.addon.llm.ExtensionLlm");
        }

        // Some ZAP versions/add-ons may not register the extension under the expected name.
        // Fall back to scanning all loaded extensions.
        if (ext == null && loader != null) {
            try {
                Object extensionsObj = tryInvokeNoArg(loader, "getExtensions");
                if (extensionsObj == null) {
                    extensionsObj = tryInvokeNoArg(loader, "getAllExtensions");
                }
                if (extensionsObj == null) {
                    extensionsObj = tryInvokeNoArg(loader, "getExtensionList");
                }

                if (extensionsObj != null) {
                    if (extensionsObj instanceof Iterable<?>) {
                        for (Object o : (Iterable<?>) extensionsObj) {
                            Extension e = o instanceof Extension ? (Extension) o : null;
                            Extension match = matchLlmExtension(e);
                            if (match != null) {
                                ext = match;
                                break;
                            }
                        }
                    } else if (extensionsObj.getClass().isArray()) {
                        int len = Array.getLength(extensionsObj);
                        for (int i = 0; i < len; i++) {
                            Object o = Array.get(extensionsObj, i);
                            Extension e = o instanceof Extension ? (Extension) o : null;
                            Extension match = matchLlmExtension(e);
                            if (match != null) {
                                ext = match;
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Ignore and keep null.
            }
        }

        // Only cache a successful lookup; a null result means the add-on hasn't loaded yet
        // and should be re-checked on the next call rather than remembered as permanently absent.
        if (ext != null) {
            this.extensionLlm = ext;
        }
        return ext;
    }

    private static Object tryInvokeNoArg(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static Extension matchLlmExtension(Extension e) {
        if (e == null) {
            return null;
        }

        String name = null;
        try {
            name = e.getName();
        } catch (Exception ignored) {
            // Ignore.
        }

        String className = e.getClass().getName();
        String simpleName = e.getClass().getSimpleName();

        if (EXTENSION_LLM_NAME.equalsIgnoreCase(name)
                || "LLM".equalsIgnoreCase(name)
                || EXTENSION_LLM_NAME.equals(simpleName)
                || className.endsWith(EXTENSION_LLM_CLASSNAME_SUFFIX)) {
            return e;
        }
        return null;
    }

    @Override
    public boolean isConfigured() {
        Extension ext = getExtensionLlm();
        if (ext == null) {
            return false;
        }
        try {
            Object result = ext.getClass().getMethod("isConfigured").invoke(ext);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getCommsIssue() {
        Extension ext = getExtensionLlm();
        if (ext == null) {
            return Constant.messages.getString("aitrafficanalyst.llm.missing.detailed");
        }
        try {
            Object result = ext.getClass().getMethod("getCommsIssue").invoke(ext);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String getModelName() {
        Extension ext = getExtensionLlm();
        if (ext == null) {
            return "";
        }
        try {
            Object result = ext.getClass().getMethod("getDefaultModelName").invoke(ext);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String chat(String prompt) throws Exception {
        if (prompt == null) {
            prompt = "";
        }

        Extension ext = getExtensionLlm();
        if (ext == null) {
            throw new IllegalStateException(
                    Constant.messages.getString("aitrafficanalyst.llm.missing.detailed"));
        }

        Object comms =
                ext.getClass()
                        .getMethod("getCommunicationService", String.class, String.class)
                        .invoke(ext, COMMS_KEY, OUTPUT_TAB_NAME);

        if (comms == null) {
            String issue = getCommsIssue();
            if (issue != null && !issue.trim().isEmpty()) {
                throw new IllegalStateException(issue);
            }
            throw new IllegalStateException(
                    Constant.messages.getString("aitrafficanalyst.llm.notConfigured"));
        }

        try {
            Object response =
                    comms.getClass().getMethod("chat", String.class).invoke(comms, prompt);
            return response != null ? response.toString() : "";
        } catch (InvocationTargetException e) {
            // Method.invoke wraps whatever the LLM add-on's chat() threw (bad API key, network
            // error, rate limit, ...) in an InvocationTargetException whose own getMessage() is
            // null, so unwrap it -- otherwise the real failure reason never reaches the panel.
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new Exception(cause.getMessage(), cause);
        } finally {
            // The LLM add-on caches one LlmCommunicationService per comms key and retains a
            // rolling window of prior chat turns on it, intended for its own interactive
            // multi-turn chat UI. Every analysis we send is already a fully self-contained
            // prompt (we build our own bounded session-context summary), so evict the cached
            // service after each call. Otherwise every subsequent analysis silently resends
            // several previous analyses' full request/response bodies as "history", inflating
            // token cost and leaking one page's traffic content into another page's analysis.
            // This only drops our cached service reference; any visible chat tab in the LLM
            // add-on's UI is left in place for transparency.
            evictCommsService(ext);
        }
    }

    private void evictCommsService(Extension ext) {
        try {
            ext.getClass()
                    .getMethod("removeCommunicationService", String.class)
                    .invoke(ext, COMMS_KEY);
        } catch (Exception ignored) {
            // Best-effort cleanup; if unavailable the next call simply reuses the accumulated
            // memory, which is a cost/robustness concern but not a functional failure.
        }
    }
}
