package com.mock.agent.match;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.agent.MockCase;

import java.util.Map;

public class RequestBodyMatcher implements Matcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean matches(MockCase mockCase, MatchContext context) {
        String expectedBody = mockCase.getRequestBody();
        if (expectedBody == null) {
            return true;
        }

        String actualBody = context.getRequestBody();
        if (actualBody == null) {
            return false;
        }

        Map<String, Object> expectedMap = parseJson(expectedBody);
        Map<String, Object> actualMap = parseJson(actualBody);

        if (expectedMap != null && actualMap != null) {
            return containsAll(actualMap, expectedMap);
        }

        return expectedBody.equals(actualBody);
    }

    @Override
    public int priority() {
        return 85;
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
