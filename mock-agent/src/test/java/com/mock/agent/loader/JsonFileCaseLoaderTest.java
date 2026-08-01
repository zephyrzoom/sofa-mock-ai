package com.mock.agent.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonFileCaseLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadValidJsonFile() throws IOException {
        String json = "{\n" +
                "  \"method\": \"POST\",\n" +
                "  \"path\": \"/user/query\",\n" +
                "  \"requestBody\": \"{\\\"userId\\\":\\\"123\\\"}\",\n" +
                "  \"response\": {\n" +
                "    \"status\": 200,\n" +
                "    \"body\": \"{\\\"name\\\":\\\"张三\\\"}\"\n" +
                "  }\n" +
                "}";
        Files.write(tempDir.resolve("user-query.json"), json.getBytes(StandardCharsets.UTF_8));

        JsonFileCaseLoader loader = new JsonFileCaseLoader(tempDir.toString());
        List<CaseLoader.LoadedCase> cases = loader.loadAll();

        assertThat(cases).hasSize(1);
        CaseLoader.LoadedCase lc = cases.get(0);
        assertThat(lc.getMethod()).isEqualTo("POST");
        assertThat(lc.getPath()).isEqualTo("/user/query");
        assertThat(lc.getRequestBody()).isEqualTo("{\"userId\":\"123\"}");
        assertThat(lc.getStatus()).isEqualTo(200);
        assertThat(lc.getBody()).isEqualTo("{\"name\":\"张三\"}");
        assertThat(lc.getFileName()).isEqualTo("user-query.json");
    }

    @Test
    void shouldSkipInvalidJsonFile() throws IOException {
        String invalidJson = "not valid json";
        Files.write(tempDir.resolve("invalid.json"), invalidJson.getBytes(StandardCharsets.UTF_8));

        JsonFileCaseLoader loader = new JsonFileCaseLoader(tempDir.toString());
        List<CaseLoader.LoadedCase> cases = loader.loadAll();

        assertThat(cases).isEmpty();
    }

    @Test
    void shouldSkipFileWithMissingFields() throws IOException {
        String json = "{\n" +
                "  \"method\": \"POST\"\n" +
                "}";
        Files.write(tempDir.resolve("incomplete.json"), json.getBytes(StandardCharsets.UTF_8));

        JsonFileCaseLoader loader = new JsonFileCaseLoader(tempDir.toString());
        List<CaseLoader.LoadedCase> cases = loader.loadAll();

        assertThat(cases).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForNonExistentDir() {
        JsonFileCaseLoader loader = new JsonFileCaseLoader("/non/existent/path");
        List<CaseLoader.LoadedCase> cases = loader.loadAll();

        assertThat(cases).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForEmptyDir() {
        JsonFileCaseLoader loader = new JsonFileCaseLoader(tempDir.toString());
        List<CaseLoader.LoadedCase> cases = loader.loadAll();

        assertThat(cases).isEmpty();
    }

    @Test
    void shouldIgnoreNonJsonFiles() throws IOException {
        Files.write(tempDir.resolve("readme.txt"), "not a json file".getBytes(StandardCharsets.UTF_8));

        JsonFileCaseLoader loader = new JsonFileCaseLoader(tempDir.toString());
        List<CaseLoader.LoadedCase> cases = loader.loadAll();

        assertThat(cases).isEmpty();
    }

    @Test
    void shouldLoadMultipleFiles() throws IOException {
        String json1 = "{\"method\":\"GET\",\"path\":\"/api/a\",\"response\":{\"status\":200,\"body\":\"ok\"}}";
        String json2 = "{\"method\":\"POST\",\"path\":\"/api/b\",\"response\":{\"status\":201,\"body\":\"created\"}}";
        Files.write(tempDir.resolve("a.json"), json1.getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("b.json"), json2.getBytes(StandardCharsets.UTF_8));

        JsonFileCaseLoader loader = new JsonFileCaseLoader(tempDir.toString());
        List<CaseLoader.LoadedCase> cases = loader.loadAll();

        assertThat(cases).hasSize(2);
    }

    @Test
    void shouldHandleOptionalRequestBody() throws IOException {
        String json = "{\"method\":\"GET\",\"path\":\"/api/test\",\"response\":{\"status\":200,\"body\":\"ok\"}}";
        Files.write(tempDir.resolve("no-body.json"), json.getBytes(StandardCharsets.UTF_8));

        JsonFileCaseLoader loader = new JsonFileCaseLoader(tempDir.toString());
        List<CaseLoader.LoadedCase> cases = loader.loadAll();

        assertThat(cases).hasSize(1);
        assertThat(cases.get(0).getRequestBody()).isNull();
    }
}
