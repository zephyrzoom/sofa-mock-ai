package com.mock.agent.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.agent.log.MockAgentLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RemoteCaseSource implements CaseSource {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String serverUrl;
    private final String appName;

    public RemoteCaseSource(String serverUrl, String appName) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.appName = appName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CaseLoader.LoadedCase> loadCases() {
        try {
            String urlStr = serverUrl + "/api/apps/" + appName + "/cases/json";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                MockAgentLogger.warn("Failed to fetch remote cases: HTTP " + responseCode);
                return Collections.emptyList();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            List<Map<String, Object>> jsonList = MAPPER.readValue(response.toString(), List.class);
            List<CaseLoader.LoadedCase> result = new ArrayList<>();

            for (Map<String, Object> json : jsonList) {
                String method = (String) json.get("method");
                String path = (String) json.get("path");
                Map<String, Object> responseMap = (Map<String, Object>) json.get("response");

                if (method == null || path == null || responseMap == null) {
                    continue;
                }

                String pathPattern = (String) json.get("pathPattern");
                String matchType = (String) json.get("matchType");
                String requestBody = (String) json.get("requestBody");
                String condition = (String) json.get("condition");
                Integer status = (Integer) responseMap.get("status");
                String body = (String) responseMap.get("body");

                CaseLoader.LoadedCase lc = new CaseLoader.LoadedCase(
                        "remote:" + appName + ":" + method + ":" + path,
                        method, path, requestBody, condition,
                        status != null ? status : 200, body,
                        pathPattern, matchType,
                        null, null, 0, null, true, 0
                );
                result.add(lc);
            }

            MockAgentLogger.info("Loaded " + result.size() + " remote cases for app: " + appName);
            return result;
        } catch (Exception e) {
            MockAgentLogger.error("Failed to load remote cases", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean supportsHotReload() {
        return true;
    }
}
