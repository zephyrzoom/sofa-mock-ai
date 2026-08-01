package com.mock.agent.api;

public class MatchEvent {

    private final long timestamp;
    private final String method;
    private final String path;
    private final String requestBodySummary;
    private final boolean matched;
    private final String caseId;
    private final long matchTimeMs;
    private final int responseStatus;
    private final String failReason;

    public MatchEvent(long timestamp, String method, String path, String requestBodySummary,
                      boolean matched, String caseId, long matchTimeMs, int responseStatus, String failReason) {
        this.timestamp = timestamp;
        this.method = method;
        this.path = path;
        this.requestBodySummary = requestBodySummary;
        this.matched = matched;
        this.caseId = caseId;
        this.matchTimeMs = matchTimeMs;
        this.responseStatus = responseStatus;
        this.failReason = failReason;
    }

    public long getTimestamp() { return timestamp; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getRequestBodySummary() { return requestBodySummary; }
    public boolean isMatched() { return matched; }
    public String getCaseId() { return caseId; }
    public long getMatchTimeMs() { return matchTimeMs; }
    public int getResponseStatus() { return responseStatus; }
    public String getFailReason() { return failReason; }

    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"timestamp\":").append(timestamp);
        sb.append(",\"method\":\"").append(method).append("\"");
        sb.append(",\"path\":\"").append(path).append("\"");
        if (requestBodySummary != null) {
            sb.append(",\"requestBodySummary\":\"").append(escapeJson(requestBodySummary)).append("\"");
        }
        sb.append(",\"matched\":").append(matched);
        if (caseId != null) {
            sb.append(",\"caseId\":\"").append(caseId).append("\"");
        }
        sb.append(",\"matchTimeMs\":").append(matchTimeMs);
        sb.append(",\"responseStatus\":").append(responseStatus);
        if (failReason != null) {
            sb.append(",\"failReason\":\"").append(escapeJson(failReason)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
