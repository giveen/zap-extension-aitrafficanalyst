package org.zaproxy.zap.extension.aitrafficanalyst.ai;

/**
 * Internal abstraction over the LLM implementation used by this add-on.
 *
 * <p>Phase 1 uses the official ZAP LLM add-on via reflection, to avoid requiring a
 * compile-time dependency on the llm add-on artifact.
 */
public interface AnalystLlmClient {

    /** @return {@code true} if the LLM add-on is present and configured. */
    boolean isConfigured();

    /**
     * @return a human-friendly issue description if unconfigured, otherwise an empty string.
     */
    String getCommsIssue();

    /**
     * Sends a single prompt to the configured model.
     *
     * @param prompt the full prompt to send.
     * @return the response text.
     * @throws Exception if the request fails.
     */
    String chat(String prompt) throws Exception;
}
