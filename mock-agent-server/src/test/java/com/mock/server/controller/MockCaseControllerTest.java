package com.mock.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.server.entity.MockCaseEntity;
import com.mock.server.repository.MockCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class MockCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockCaseRepository repository;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testCreateAndGetCase() throws Exception {
        MockCaseEntity entity = new MockCaseEntity();
        entity.setAppName("test-app");
        entity.setMethod("GET");
        entity.setPath("/api/users");
        entity.setStatus(200);
        entity.setBody("{\"users\": []}");
        entity.setEnabled(true);

        String json = mapper.writeValueAsString(entity);

        // Create
        mockMvc.perform(post("/api/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appName").value("test-app"))
                .andExpect(jsonPath("$.method").value("GET"));

        // List
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("/api/users"));
    }

    @Test
    void testUpdateCase() throws Exception {
        MockCaseEntity entity = new MockCaseEntity();
        entity.setAppName("test-app");
        entity.setMethod("POST");
        entity.setPath("/api/orders");
        entity.setStatus(201);
        entity.setBody("{\"orderId\": 1}");
        entity.setEnabled(true);
        entity = repository.save(entity);

        entity.setStatus(200);
        String json = mapper.writeValueAsString(entity);

        mockMvc.perform(put("/api/cases/" + entity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void testDeleteCase() throws Exception {
        MockCaseEntity entity = new MockCaseEntity();
        entity.setAppName("test-app");
        entity.setMethod("DELETE");
        entity.setPath("/api/items/1");
        entity.setStatus(200);
        entity.setBody("{}");
        entity.setEnabled(true);
        entity = repository.save(entity);

        mockMvc.perform(delete("/api/cases/" + entity.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cases/" + entity.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testFilterByAppName() throws Exception {
        MockCaseEntity e1 = new MockCaseEntity();
        e1.setAppName("app-a");
        e1.setMethod("GET");
        e1.setPath("/a");
        e1.setStatus(200);
        e1.setBody("{}");
        e1.setEnabled(true);
        repository.save(e1);

        MockCaseEntity e2 = new MockCaseEntity();
        e2.setAppName("app-b");
        e2.setMethod("GET");
        e2.setPath("/b");
        e2.setStatus(200);
        e2.setBody("{}");
        e2.setEnabled(true);
        repository.save(e2);

        mockMvc.perform(get("/api/cases").param("appName", "app-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appName").value("app-a"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testUploadCases() throws Exception {
        String casesJson = "[{\"appName\":\"upload-app\",\"method\":\"GET\",\"path\":\"/test\",\"status\":200,\"body\":\"{}\",\"enabled\":true}]";

        MockMultipartFile file = new MockMultipartFile(
                "file", "cases.json", "application/json", casesJson.getBytes());

        mockMvc.perform(multipart("/api/cases/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));
    }

    @Test
    void testExportCases() throws Exception {
        MockCaseEntity entity = new MockCaseEntity();
        entity.setAppName("export-app");
        entity.setMethod("GET");
        entity.setPath("/export");
        entity.setStatus(200);
        entity.setBody("{\"data\": true}");
        entity.setEnabled(true);
        repository.save(entity);

        mockMvc.perform(get("/api/cases/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appName").value("export-app"));
    }

    @Test
    void testListApps() throws Exception {
        MockCaseEntity e1 = new MockCaseEntity();
        e1.setAppName("app-x");
        e1.setMethod("GET");
        e1.setPath("/x");
        e1.setStatus(200);
        e1.setBody("{}");
        e1.setEnabled(true);
        repository.save(e1);

        MockCaseEntity e2 = new MockCaseEntity();
        e2.setAppName("app-y");
        e2.setMethod("GET");
        e2.setPath("/y");
        e2.setStatus(200);
        e2.setBody("{}");
        e2.setEnabled(true);
        repository.save(e2);

        mockMvc.perform(get("/api/cases/apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
