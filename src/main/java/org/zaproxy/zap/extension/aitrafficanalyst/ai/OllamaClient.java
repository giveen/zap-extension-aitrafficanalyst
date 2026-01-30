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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OllamaClient {

    private static final Logger LOGGER = LogManager.getLogger(OllamaClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String generateEndpoint;
    private final String tagsEndpoint;

    // Updated Constructor
    public OllamaClient(String baseUrl) {
        // Keep original base for reference
        this.baseUrl = baseUrl;
        // Normalize to ensure we can build endpoints without duplicating paths
        String b = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        // Detect if user provided a full endpoint (contains /api/generate or /api/tags)
        if (b.contains("/api/generate")) {
            int idx = b.indexOf("/api/generate");
            this.generateEndpoint = b.substring(0, idx) + "/api/generate";
            this.tagsEndpoint = this.generateEndpoint.replace("/api/generate", "/api/tags");
        } else if (b.contains("/api/tags")) {
            int idx = b.indexOf("/api/tags");
            this.tagsEndpoint = b.substring(0, idx) + "/api/tags";
            this.generateEndpoint = this.tagsEndpoint.replace("/api/tags", "/api/generate");
        } else {
            // Treat input as a base URL and append paths
            this.generateEndpoint = b + "api/generate";
            this.tagsEndpoint = b + "api/tags";
        }
        this.client =
                new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(120, TimeUnit.SECONDS) // AI can be slow
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build();
        this.mapper = new ObjectMapper();
    }

    /** Construct an OllamaClient using a shared OkHttpClient instance. */
    public OllamaClient(OkHttpClient sharedClient, String baseUrl) {
        this.baseUrl = baseUrl;
        String b = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";

        if (b.contains("/api/generate")) {
            int idx = b.indexOf("/api/generate");
            this.generateEndpoint = b.substring(0, idx) + "/api/generate";
            this.tagsEndpoint = this.generateEndpoint.replace("/api/generate", "/api/tags");
        } else if (b.contains("/api/tags")) {
            int idx = b.indexOf("/api/tags");
            this.tagsEndpoint = b.substring(0, idx) + "/api/tags";
            this.generateEndpoint = this.tagsEndpoint.replace("/api/tags", "/api/generate");
        } else {
            this.generateEndpoint = b + "api/generate";
            this.tagsEndpoint = b + "api/tags";
        }

        this.client = sharedClient;
        this.mapper = new ObjectMapper();
    }

    public List<String> getModels() throws IOException {
        Request request = new Request.Builder().url(this.tagsEndpoint).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch models: " + response);
            }

            String responseBody = response.body().string();
            JsonNode rootNode = mapper.readTree(responseBody);
            List<String> models = new ArrayList<>();

            if (rootNode.has("models")) {
                Iterator<JsonNode> elements = rootNode.get("models").elements();
                while (elements.hasNext()) {
                    JsonNode model = elements.next();
                    models.add(model.get("name").asText());
                }
            }
            return models;
        }
    }

    public String query(String modelName, String promptText) throws IOException {
        // Build JSON Payload
        // We use "stream": false so we get the full response at once
        String jsonBody =
                mapper.createObjectNode()
                        .put("model", modelName)
                        .put("prompt", promptText)
                        .put("stream", false)
                        .toString();

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder().url(this.generateEndpoint).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JsonNode rootNode = mapper.readTree(responseBody);

            // Extract the actual 'response' text from Ollama's JSON
            if (rootNode.has("response")) {
                return rootNode.get("response").asText();
            } else {
                return "Error: No 'response' field in Ollama output.";
            }
        }
    }
}
