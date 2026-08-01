package com.mock.agent.match;

import com.mock.agent.MockCase;

import java.util.Map;

public class HeaderMatcher implements Matcher {

    @Override
    public boolean matches(MockCase mockCase, MatchContext context) {
        Map<String, String> expectedHeaders = mockCase.getRequestHeaders();
        if (expectedHeaders == null || expectedHeaders.isEmpty()) {
            return true;
        }

        Map<String, String> actualHeaders = context.getRequestHeaders();
        for (Map.Entry<String, String> entry : expectedHeaders.entrySet()) {
            String expectedKey = entry.getKey();
            String expectedValue = entry.getValue();
            String actualValue = actualHeaders.get(expectedKey);
            if (actualValue == null || !actualValue.equals(expectedValue)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int priority() {
        return 90;
    }
}
