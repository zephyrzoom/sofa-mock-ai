package com.mock.agent.match;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionEvaluatorTest {

    @Test
    void shouldReturnTrueForNullCondition() {
        boolean result = ConditionEvaluator.evaluate(null, "{}", Collections.emptyMap());
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForEmptyCondition() {
        boolean result = ConditionEvaluator.evaluate("", "{}", Collections.emptyMap());
        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluateEqualsCondition() {
        String body = "{\"userId\":\"123\"}";
        boolean result = ConditionEvaluator.evaluate("body.userId == '123'", body, Collections.emptyMap());
        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluateNotEqualsCondition() {
        String body = "{\"userId\":\"123\"}";
        boolean result = ConditionEvaluator.evaluate("body.userId != '456'", body, Collections.emptyMap());
        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluateContainsCondition() {
        String body = "{\"name\":\"张三\"}";
        boolean result = ConditionEvaluator.evaluate("body.name contains '张'", body, Collections.emptyMap());
        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluateHeaderCondition() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Env", "test");
        boolean result = ConditionEvaluator.evaluate("headers['X-Env'] == 'test'", "{}", headers);
        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluateAndCondition() {
        String body = "{\"userId\":\"123\",\"name\":\"张三\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Env", "test");
        boolean result = ConditionEvaluator.evaluate(
                "body.userId == '123' && headers['X-Env'] == 'test'", body, headers);
        assertThat(result).isTrue();
    }

    @Test
    void shouldEvaluateOrCondition() {
        String body = "{\"userId\":\"123\"}";
        boolean result = ConditionEvaluator.evaluate(
                "body.userId == '123' || body.userId == '456'", body, Collections.emptyMap());
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseForMismatch() {
        String body = "{\"userId\":\"123\"}";
        boolean result = ConditionEvaluator.evaluate("body.userId == '456'", body, Collections.emptyMap());
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseForInvalidCondition() {
        boolean result = ConditionEvaluator.evaluate("invalid!!!", "{}", Collections.emptyMap());
        assertThat(result).isFalse();
    }
}
