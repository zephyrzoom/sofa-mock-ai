package com.mock.agent.match;

import com.mock.agent.MockCase;

public interface Matcher {

    boolean matches(MockCase mockCase, MatchContext context);

    int priority();

    class MatchContext {
        private final String method;
        private final String path;
        private final String requestBody;
        private final java.util.Map<String, String> requestHeaders;

        public MatchContext(String method, String path, String requestBody, java.util.Map<String, String> requestHeaders) {
            this.method = method;
            this.path = path;
            this.requestBody = requestBody;
            this.requestHeaders = requestHeaders != null ? requestHeaders : java.util.Collections.emptyMap();
        }

        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getRequestBody() { return requestBody; }
        public java.util.Map<String, String> getRequestHeaders() { return requestHeaders; }
    }
}
