package org.zaproxy.zap.extension.aitrafficanalyst.ai;

import java.lang.reflect.Array;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.ExtensionLoader;

/**
 * Uses the official ZAP LLM add-on (ExtensionLlm) via reflection.
 *
 * <p>This project is a standalone add-on and does not have a published Maven dependency for the
 * LLM add-on, so we avoid compile-time linking.
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

        ExtensionLoader loader = Control.getSingleton().getExtensionLoader();
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

        this.extensionLlm = ext;
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
    public String chat(String prompt) throws Exception {
        if (prompt == null) {
            prompt = "";
        }

        Extension ext = getExtensionLlm();
        if (ext == null) {
            throw new IllegalStateException(
                    Constant.messages.getString("aitrafficanalyst.llm.missing.detailed"));
        }

        Object comms = ext.getClass()
                .getMethod("getCommunicationService", String.class, String.class)
                .invoke(ext, COMMS_KEY, OUTPUT_TAB_NAME);

        if (comms == null) {
            String issue = getCommsIssue();
            if (issue != null && !issue.trim().isEmpty()) {
                throw new IllegalStateException(issue);
            }
            throw new IllegalStateException(Constant.messages.getString("aitrafficanalyst.llm.notConfigured"));
        }

        Object response = comms.getClass().getMethod("chat", String.class).invoke(comms, prompt);
        return response != null ? response.toString() : "";
    }
}
