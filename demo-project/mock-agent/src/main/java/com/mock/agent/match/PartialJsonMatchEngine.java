package com.mock.agent.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.agent.MockCase;

import java.util.List;
import java.util.Map;

public class PartialJsonMatchEngine implements MatchEngine {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public MockCase findMatch(String method, String path, String requestBody, List<MockCase> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // Priority 1: request body partial field match
        if (requestBody != null) {
            Map<String, Object> actualBody = parseJson(requestBody);
            for (MockCase mc : candidates) {
                if (mc.getRequestBody() != null) {
                    Map<String, Object> expectedBody = parseJson(mc.getRequestBody());
                    if (expectedBody != null && actualBody != null) {
                        if (containsAll(actualBody, expectedBody)) {
                            return mc;
                        }
                    } else if (mc.getRequestBody().equals(requestBody)) {
                        return mc;
                    }
                }
            }
        }

        // Priority 2: catch-all (no requestBody constraint)
        for (MockCase mc : candidates) {
            if (mc.getRequestBody() == null) {
                return mc;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String json) {
        try {
            Object obj = MAPPER.readValue(json, Object.class);
            if (obj instanceof Map) {
                return (Map<String, Object>) obj;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean containsAll(Map<String, Object> actual, Map<String, Object> expected) {
        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            String key = entry.getKey();
            Object expectedVal = entry.getValue();
            if (!actual.containsKey(key)) {
                return false;
            }
            Object actualVal = actual.get(key);
            if (actualVal == null && expectedVal == null) {
                continue;
            }
            if (actualVal == null || expectedVal == null) {
                return false;
            }
            if (expectedVal instanceof Map && actualVal instanceof Map) {
                if (!containsAll((Map<String, Object>) actualVal, (Map<String, Object>) expectedVal)) {
                    return false;
                }
            } else if (!expectedVal.equals(actualVal)) {
                return false;
            }
        }
        return true;
    }
}
