package com.mock.agent.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.agent.log.MockAgentLogger;

import java.nio.charset.StandardCharsets;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonFileCaseLoader implements CaseLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private String casesDirPath;

    public JsonFileCaseLoader() {
        this.casesDirPath = System.getProperty("mock.cases.dir", "mock-cases");
    }

    public JsonFileCaseLoader(String casesDirPath) {
        this.casesDirPath = casesDirPath;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LoadedCase> loadAll() {
        List<LoadedCase> result = new ArrayList<>();
        File dir = new File(casesDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return result;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return result;
        }

        for (File file : files) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                Map<String, Object> json = MAPPER.readValue(reader, Map.class);
                String method = (String) json.get("method");
                String path = (String) json.get("path");
                Map<String, Object> response = (Map<String, Object>) json.get("response");
                if (method == null || path == null || response == null) {
                    MockAgentLogger.warn("skipping invalid mock case: " + file.getName());
                    continue;
                }

                // Parse optional fields
                String pathPattern = (String) json.get("pathPattern");
                String matchType = (String) json.get("matchType");
                Map<String, String> requestHeaders = (Map<String, String>) json.get("requestHeaders");
                Map<String, String> responseHeaders = (Map<String, String>) json.get("responseHeaders");
                Long delayMs = json.get("delayMs") != null ? ((Number) json.get("delayMs")).longValue() : 0;
                String description = (String) json.get("description");
                Boolean enabled = json.get("enabled") != null ? (Boolean) json.get("enabled") : true;
                Integer priority = json.get("priority") != null ? ((Number) json.get("priority")).intValue() : 0;
                String condition = (String) json.get("condition");

                LoadedCase lc = new LoadedCase(
                        file.getName(),
                        method,
                        path,
                        (String) json.get("requestBody"),
                        condition,
                        (Integer) response.get("status"),
                        (String) response.get("body"),
                        pathPattern,
                        matchType,
                        requestHeaders,
                        responseHeaders,
                        delayMs,
                        description,
                        enabled,
                        priority
                );
                result.add(lc);
            } catch (IOException e) {
                MockAgentLogger.error("failed to parse: " + file.getName());
            }
        }

        return result;
    }
}
