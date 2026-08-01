package com.mock.agent.match;

import com.mock.agent.MockCase;

public class ExactPathMatcher implements PathMatcher {

    @Override
    public boolean matches(MockCase mockCase, MatchContext context) {
        String matchType = mockCase.getMatchType();
        // Skip only if explicitly set to REGEX or ANT
        if ("REGEX".equalsIgnoreCase(matchType) || "ANT".equalsIgnoreCase(matchType)) {
            return false;
        }
        String casePath = mockCase.getPath();
        return casePath != null && casePath.equals(context.getPath());
    }

    @Override
    public int priority() {
        return 100;
    }
}
