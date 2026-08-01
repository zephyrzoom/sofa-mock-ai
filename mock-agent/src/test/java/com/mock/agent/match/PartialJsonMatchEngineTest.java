package com.mock.agent.match;

import com.mock.agent.MockCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartialJsonMatchEngineTest {

    private MatchEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PartialJsonMatchEngine();
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
    void shouldMatchCatchAllWhenNoRequestBody() {
        MockCase catchAll = createCase(null, 200, "{\"status\":\"ok\"}");
        List<MockCase> candidates = Collections.singletonList(catchAll);

        MockCase result = engine.findMatch("GET", "/api/test", null, candidates);
        assertThat(result).isSameAs(catchAll);
    }

    @Test
    void shouldMatchCatchAllWithRequestBodyProvided() {
        MockCase catchAll = createCase(null, 200, "{\"status\":\"ok\"}");
        List<MockCase> candidates = Collections.singletonList(catchAll);

        MockCase result = engine.findMatch("POST", "/api/test", "{\"userId\":\"123\"}", candidates);
        assertThat(result).isSameAs(catchAll);
    }

    @Test
    void shouldMatchExactRequestBody() {
        MockCase exactCase = createCase("{\"userId\":\"123\"}", 200, "{\"name\":\"张三\"}");
        List<MockCase> candidates = Collections.singletonList(exactCase);

        MockCase result = engine.findMatch("POST", "/api/test", "{\"userId\":\"123\"}", candidates);
        assertThat(result).isSameAs(exactCase);
    }

    @Test
    void shouldNotMatchDifferentRequestBody() {
        MockCase case123 = createCase("{\"userId\":\"123\"}", 200, "{\"name\":\"张三\"}");
        MockCase case456 = createCase("{\"userId\":\"456\"}", 200, "{\"name\":\"李四\"}");
        List<MockCase> candidates = Arrays.asList(case123, case456);

        MockCase result = engine.findMatch("POST", "/api/test", "{\"userId\":\"789\"}", candidates);
        assertThat(result).isNull();
    }

    @Test
    void shouldMatchPartialRequestBodyFields() {
        MockCase partialCase = createCase("{\"userId\":\"123\"}", 200, "{\"name\":\"张三\"}");
        List<MockCase> candidates = Collections.singletonList(partialCase);

        String actualBody = "{\"userId\":\"123\",\"extra\":\"field\"}";
        MockCase result = engine.findMatch("POST", "/api/test", actualBody, candidates);
        assertThat(result).isSameAs(partialCase);
    }

    @Test
    void shouldMatchNestedObjectPartialFields() {
        MockCase nestedCase = createCase("{\"user\":{\"id\":\"123\"}}", 200, "{\"ok\":true}");
        List<MockCase> candidates = Collections.singletonList(nestedCase);

        String actualBody = "{\"user\":{\"id\":\"123\",\"name\":\"test\"},\"extra\":true}";
        MockCase result = engine.findMatch("POST", "/api/test", actualBody, candidates);
        assertThat(result).isSameAs(nestedCase);
    }

    @Test
    void shouldPreferBodyMatchOverCatchAll() {
        MockCase catchAll = createCase(null, 200, "{\"default\":true}");
        MockCase bodyMatch = createCase("{\"userId\":\"123\"}", 200, "{\"name\":\"张三\"}");
        List<MockCase> candidates = Arrays.asList(catchAll, bodyMatch);

        MockCase result = engine.findMatch("POST", "/api/test", "{\"userId\":\"123\"}", candidates);
        assertThat(result).isSameAs(bodyMatch);
    }

    @Test
    void shouldFallbackToCatchAllWhenBodyNotMatched() {
        MockCase bodyMatch = createCase("{\"userId\":\"123\"}", 200, "{\"name\":\"张三\"}");
        MockCase catchAll = createCase(null, 200, "{\"default\":true}");
        List<MockCase> candidates = Arrays.asList(bodyMatch, catchAll);

        MockCase result = engine.findMatch("POST", "/api/test", "{\"userId\":\"999\"}", candidates);
        assertThat(result).isSameAs(catchAll);
    }

    @Test
    void shouldFallbackToExactMatchForInvalidJson() {
        String invalidJson = "not-json";
        MockCase exactCase = createCase(invalidJson, 200, "{\"ok\":true}");
        List<MockCase> candidates = Collections.singletonList(exactCase);

        MockCase result = engine.findMatch("POST", "/api/test", invalidJson, candidates);
        assertThat(result).isSameAs(exactCase);
    }

    @Test
    void shouldNotMatchWhenActualMissingExpectedField() {
        MockCase caseWithField = createCase("{\"userId\":\"123\"}", 200, "{\"ok\":true}");
        List<MockCase> candidates = Collections.singletonList(caseWithField);

        MockCase result = engine.findMatch("POST", "/api/test", "{\"other\":\"value\"}", candidates);
        assertThat(result).isNull();
    }

    private MockCase createCase(String requestBody, int status, String body) {
        MockCase mc = new MockCase();
        mc.setMethod("POST");
        mc.setPath("/api/test");
        mc.setRequestBody(requestBody);
        mc.setStatus(status);
        mc.setBody(body);
        return mc;
    }
}
