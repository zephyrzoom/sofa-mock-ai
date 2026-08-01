package com.mock.agent.loader;

import java.util.List;
import java.util.Map;

public interface CaseLoader {

    List<LoadedCase> loadAll();

    class LoadedCase {
        private final String fileName;
        private final String method;
        private final String path;
        private final String requestBody;
        private final String condition;
        private final int status;
        private final String body;
        private final String pathPattern;
        private final String matchType;
        private final Map<String, String> requestHeaders;
        private final Map<String, String> responseHeaders;
        private final long delayMs;
        private final String description;
        private final boolean enabled;
        private final int priority;

        public LoadedCase(String fileName, String method, String path, String requestBody, int status, String body) {
            this(fileName, method, path, requestBody, null, status, body, null, null, null, null, 0, null, true, 0);
        }

        public LoadedCase(String fileName, String method, String path, String requestBody, String condition,
                          int status, String body,
                          String pathPattern, String matchType,
                          Map<String, String> requestHeaders, Map<String, String> responseHeaders,
                          long delayMs, String description, boolean enabled, int priority) {
            this.fileName = fileName;
            this.method = method;
            this.path = path;
            this.requestBody = requestBody;
            this.condition = condition;
            this.status = status;
            this.body = body;
            this.pathPattern = pathPattern;
            this.matchType = matchType;
            this.requestHeaders = requestHeaders;
            this.responseHeaders = responseHeaders;
            this.delayMs = delayMs;
            this.description = description;
            this.enabled = enabled;
            this.priority = priority;
        }

        public String getFileName() { return fileName; }
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getRequestBody() { return requestBody; }
        public String getCondition() { return condition; }
        public int getStatus() { return status; }
        public String getBody() { return body; }
        public String getPathPattern() { return pathPattern; }
        public String getMatchType() { return matchType; }
        public Map<String, String> getRequestHeaders() { return requestHeaders; }
        public Map<String, String> getResponseHeaders() { return responseHeaders; }
        public long getDelayMs() { return delayMs; }
        public String getDescription() { return description; }
        public boolean isEnabled() { return enabled; }
        public int getPriority() { return priority; }
    }
}
