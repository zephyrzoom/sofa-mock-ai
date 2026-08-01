package com.mock.agent.match;

import com.mock.agent.MockCase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexPathMatcher implements PathMatcher {

    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean matches(MockCase mockCase, MatchContext context) {
        String pattern = mockCase.getPathPattern();
        if (pattern == null || !"REGEX".equalsIgnoreCase(mockCase.getMatchType())) {
            return false;
        }

        Pattern compiled = PATTERN_CACHE.computeIfAbsent(pattern, p -> {
            try {
                return Pattern.compile(p);
            } catch (PatternSyntaxException e) {
                return null;
            }
        });

        return compiled != null && compiled.matcher(context.getPath()).matches();
    }

    @Override
    public int priority() {
        return 80;
    }
}
