package com.mock.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MockCaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldCreateWithDefaults() {
        MockCase mc = new MockCase();
        assertThat(mc.isEnabled()).isTrue();
        assertThat(mc.getPriority()).isEqualTo(0);
        assertThat(mc.getDelayMs()).isEqualTo(0);
        assertThat(mc.getRequestHeaders()).isEmpty();
        assertThat(mc.getResponseHeaders()).isEmpty();
    }

    @Test
    void shouldSerializeAndDeserialize() throws Exception {
        MockCase original = new MockCase();
        original.setId("test-1");
        original.setMethod("POST");
        original.setPath("/api/test");
        original.setRequestBody("{\"key\":\"value\"}");
        original.setStatus(200);
        original.setBody("{\"result\":\"ok\"}");
        original.setDescription("test case");
        original.setEnabled(true);
        original.setPriority(5);
        original.setDelayMs(100);

        Map<String, String> reqHeaders = new HashMap<>();
        reqHeaders.put("Content-Type", "application/json");
        original.setRequestHeaders(reqHeaders);

        Map<String, String> respHeaders = new HashMap<>();
        respHeaders.put("X-Custom", "test");
        original.setResponseHeaders(respHeaders);

        String json = MAPPER.writeValueAsString(original);
        MockCase deserialized = MAPPER.readValue(json, MockCase.class);

        assertThat(deserialized.getId()).isEqualTo("test-1");
        assertThat(deserialized.getMethod()).isEqualTo("POST");
        assertThat(deserialized.getPath()).isEqualTo("/api/test");
        assertThat(deserialized.getRequestBody()).isEqualTo("{\"key\":\"value\"}");
        assertThat(deserialized.getStatus()).isEqualTo(200);
        assertThat(deserialized.getBody()).isEqualTo("{\"result\":\"ok\"}");
        assertThat(deserialized.getDescription()).isEqualTo("test case");
        assertThat(deserialized.isEnabled()).isTrue();
        assertThat(deserialized.getPriority()).isEqualTo(5);
        assertThat(deserialized.getDelayMs()).isEqualTo(100);
        assertThat(deserialized.getRequestHeaders()).containsEntry("Content-Type", "application/json");
        assertThat(deserialized.getResponseHeaders()).containsEntry("X-Custom", "test");
    }

    @Test
    void shouldDeserializeWithMissingOptionalFields() throws Exception {
        String json = "{\"method\":\"GET\",\"path\":\"/api\",\"status\":200,\"body\":\"ok\"}";
        MockCase mc = MAPPER.readValue(json, MockCase.class);

        assertThat(mc.getMethod()).isEqualTo("GET");
        assertThat(mc.getPath()).isEqualTo("/api");
        assertThat(mc.getStatus()).isEqualTo(200);
        assertThat(mc.getBody()).isEqualTo("ok");
        assertThat(mc.getRequestBody()).isNull();
        assertThat(mc.isEnabled()).isTrue();
        assertThat(mc.getPriority()).isEqualTo(0);
    }
}
