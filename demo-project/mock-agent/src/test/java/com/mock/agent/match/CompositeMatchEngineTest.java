package com.mock.agent.match;

import com.mock.agent.MockCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeMatchEngineTest {

    private MatchEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CompositeMatchEngine();
    }

    @Test
    void shouldReturnNullForEmptyCandidates() {
        MockCase result = engine.findMatch("GET", "/api/test", null, Collections.emptyList());
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForNullCandidates() {
        MockCase result = engine.findMatch("GET", "/api/test", null, null);
        assertThat(result).isNull();
    }

    @Test
    void shouldMatchExactPath() {
        MockCase exactCase = createCase("/api/test", null, null, 200, "{\"ok\":true}");
        List<MockCase> candidates = Collections.singletonList(exactCase);

        MockCase result = engine.findMatch("GET", "/api/test", null, candidates);
        assertThat(result).isSameAs(exactCase);
    }

    @Test
    void shouldNotMatchDifferentPath() {
        MockCase case1 = createCase("/api/a", null, null, 200, "{\"ok\":true}");
        MockCase case2 = createCase("/api/b", null, null, 200, "{\"ok\":true}");
        List<MockCase> candidates = Arrays.asList(case1, case2);

        MockCase result = engine.findMatch("GET", "/api/c", null, candidates);
        assertThat(result).isNull();
    }

    @Test
    void shouldMatchRegexPath() {
        MockCase regexCase = createCase("/api/users/.*", "REGEX", null, 200, "{\"ok\":true}");
        List<MockCase> candidates = Collections.singletonList(regexCase);

        MockCase result = engine.findMatch("GET", "/api/users/123", null, candidates);
        assertThat(result).isSameAs(regexCase);
    }

    @Test
    void shouldMatchAntPath() {
        MockCase antCase = createCase("/api/users/{id}", "ANT", null, 200, "{\"ok\":true}");
        List<MockCase> candidates = Collections.singletonList(antCase);

        MockCase result = engine.findMatch("GET", "/api/users/456", null, candidates);
        assertThat(result).isSameAs(antCase);
    }

    @Test
    void shouldMatchWithRequestBody() {
        MockCase bodyCase = createCase("/api/test", null, "{\"userId\":\"123\"}", 200, "{\"name\":\"张三\"}");
        List<MockCase> candidates = Collections.singletonList(bodyCase);

        MockCase result = engine.findMatch("POST", "/api/test", "{\"userId\":\"123\",\"extra\":\"field\"}", candidates);
        assertThat(result).isSameAs(bodyCase);
    }

    @Test
    void shouldPreferHigherPriority() {
        MockCase lowPriority = createCase("/api/test", null, null, 200, "{\"low\":true}");
        lowPriority.setPriority(1);
        MockCase highPriority = createCase("/api/test", null, null, 200, "{\"high\":true}");
        highPriority.setPriority(10);
        List<MockCase> candidates = Arrays.asList(lowPriority, highPriority);

        MockCase result = engine.findMatch("GET", "/api/test", null, candidates);
        assertThat(result).isSameAs(highPriority);
    }

    @Test
    void shouldSkipDisabledCases() {
        MockCase disabled = createCase("/api/test", null, null, 200, "{\"ok\":true}");
        disabled.setEnabled(false);
        List<MockCase> candidates = Collections.singletonList(disabled);

        MockCase result = engine.findMatch("GET", "/api/test", null, candidates);
        assertThat(result).isNull();
    }

    private MockCase createCase(String path, String matchType, String requestBody, int status, String body) {
        MockCase mc = new MockCase();
        mc.setMethod("GET");
        mc.setPath(path);
        mc.setPathPattern(matchType != null ? path : null);
        mc.setMatchType(matchType);
        mc.setRequestBody(requestBody);
        mc.setStatus(status);
        mc.setBody(body);
        return mc;
    }
}
