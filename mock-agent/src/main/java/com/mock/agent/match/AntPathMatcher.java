package com.mock.agent.match;

import com.mock.agent.MockCase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class AntPathMatcher implements PathMatcher {

    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean matches(MockCase mockCase, MatchContext context) {
        String pattern = mockCase.getPathPattern();
        if (pattern == null || !"ANT".equalsIgnoreCase(mockCase.getMatchType())) {
            return false;
        }

        Pattern compiled = PATTERN_CACHE.computeIfAbsent(pattern, p -> Pattern.compile(toRegex(p)));
        return compiled.matcher(context.getPath()).matches();
    }

    @Override
    public int priority() {
        return 80;
    }

    private static String toRegex(String antPattern) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < antPattern.length(); i++) {
            char c = antPattern.charAt(i);
            if (c == '*') {
                if (i + 1 < antPattern.length() && antPattern.charAt(i + 1) == '*') {
                    regex.append(".*");
                    i++;
                } else {
                    regex.append("[^/]*");
                }
            } else if (c == '{') {
                int end = antPattern.indexOf('}', i);
                if (end > i) {
                    regex.append("([^/]+)");
                    i = end;
                } else {
                    regex.append(Pattern.quote(String.valueOf(c)));
                }
            } else if (c == '?') {
                regex.append("[^/]");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        regex.append("$");
        return regex.toString();
    }
}
