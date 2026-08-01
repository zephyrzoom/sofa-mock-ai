package com.mock.agent.match;

import com.mock.agent.MockCase;
import com.mock.agent.log.MockAgentLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CompositeMatchEngine implements MatchEngine {

    private final List<PathMatcher> pathMatchers;
    private final List<Matcher> otherMatchers;

    public CompositeMatchEngine() {
        this(createDefaultPathMatchers(), createDefaultOtherMatchers());
    }

    public CompositeMatchEngine(List<PathMatcher> pathMatchers, List<Matcher> otherMatchers) {
        List<PathMatcher> sortedPath = new ArrayList<>(pathMatchers);
        sortedPath.sort(Comparator.comparingInt(Matcher::priority).reversed());
        this.pathMatchers = Collections.unmodifiableList(sortedPath);

        List<Matcher> sortedOther = new ArrayList<>(otherMatchers);
        sortedOther.sort(Comparator.comparingInt(Matcher::priority).reversed());
        this.otherMatchers = Collections.unmodifiableList(sortedOther);
    }

    @Override
    public MockCase findMatch(String method, String path, String requestBody, List<MockCase> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Matcher.MatchContext context = new Matcher.MatchContext(method, path, requestBody, null);

        // Filter enabled cases and sort by priority
        List<MockCase> sorted = new ArrayList<>();
        for (MockCase mc : candidates) {
            if (mc.isEnabled()) {
                sorted.add(mc);
            }
        }
        sorted.sort(Comparator.comparingInt(MockCase::getPriority).reversed());

        // Try each case with all matchers
        for (MockCase mc : sorted) {
            if (matchesAll(mc, context)) {
                return mc;
            }
        }

        return null;
    }

    private boolean matchesAll(MockCase mockCase, Matcher.MatchContext context) {
        // Path matchers use OR logic - at least one must match
        boolean pathMatched = false;
        for (PathMatcher matcher : pathMatchers) {
            if (matcher.matches(mockCase, context)) {
                pathMatched = true;
                MockAgentLogger.debug("path matched by: " + matcher.getClass().getSimpleName());
                break;
            }
        }
        if (!pathMatched) {
            MockAgentLogger.debug("path not matched: case=" + mockCase.getPath()
                    + " pattern=" + mockCase.getPathPattern()
                    + " matchType=" + mockCase.getMatchType()
                    + " request=" + context.getPath());
            return false;
        }

        // Other matchers use AND logic - all must match
        for (Matcher matcher : otherMatchers) {
            if (!matcher.matches(mockCase, context)) {
                MockAgentLogger.info("match failed: case=" + mockCase.getMethod() + " " + mockCase.getPath()
                        + " failed at " + matcher.getClass().getSimpleName());
                return false;
            }
        }

        return true;
    }

    private static List<PathMatcher> createDefaultPathMatchers() {
        List<PathMatcher> matchers = new ArrayList<>();
        matchers.add(new ExactPathMatcher());
        matchers.add(new RegexPathMatcher());
        matchers.add(new AntPathMatcher());
        return matchers;
    }

    private static List<Matcher> createDefaultOtherMatchers() {
        List<Matcher> matchers = new ArrayList<>();
        matchers.add(new HeaderMatcher());
        matchers.add(new RequestBodyMatcher());
        matchers.add(new ConditionMatcher());
        return matchers;
    }
}
