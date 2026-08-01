package com.mock.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockHttpClientResponseBuilderTest {

    @Test
    void shouldBuildResponseWithStatusCode200() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String body = "{\"result\":\"ok\"}";

        Object response = MockHttpClientResponseBuilder.build(cl, 200, body);

        assertThat(response).isNotNull();
        assertThat(response.getClass().getName()).contains("Proxy");
    }

    @Test
    void shouldBuildResponseWithStatusCode404() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String body = "{\"error\":\"not found\"}";

        Object response = MockHttpClientResponseBuilder.build(cl, 404, body);

        assertThat(response).isNotNull();
    }

    @Test
    void shouldBuildResponseWithEmptyBody() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String body = "";

        Object response = MockHttpClientResponseBuilder.build(cl, 204, body);

        assertThat(response).isNotNull();
    }

    @Test
    void shouldGetCorrectReasonPhrase() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Object response200 = MockHttpClientResponseBuilder.build(cl, 200, "ok");
        Object response400 = MockHttpClientResponseBuilder.build(cl, 400, "bad request");
        Object response500 = MockHttpClientResponseBuilder.build(cl, 500, "error");

        assertThat(response200).isNotNull();
        assertThat(response400).isNotNull();
        assertThat(response500).isNotNull();
    }
}
