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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class RemoteCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockCaseRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testGetCasesForApp() throws Exception {
        MockCaseEntity e1 = new MockCaseEntity();
        e1.setAppName("remote-app");
        e1.setMethod("GET");
        e1.setPath("/api/data");
        e1.setStatus(200);
        e1.setBody("{\"data\": 123}");
        e1.setEnabled(true);
        repository.save(e1);

        MockCaseEntity e2 = new MockCaseEntity();
        e2.setAppName("remote-app");
        e2.setMethod("POST");
        e2.setPath("/api/submit");
        e2.setStatus(201);
        e2.setBody("{\"created\": true}");
        e2.setEnabled(false); // disabled, should not appear
        repository.save(e2);

        mockMvc.perform(get("/api/apps/remote-app/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("/api/data"));
    }

    @Test
    void testGetCasesAsJson() throws Exception {
        MockCaseEntity entity = new MockCaseEntity();
        entity.setAppName("json-app");
        entity.setMethod("GET");
        entity.setPath("/api/users");
        entity.setPathPattern("/api/users/{id}");
        entity.setMatchType("ANT");
        entity.setStatus(200);
        entity.setBody("{\"users\": []}");
        entity.setEnabled(true);
        repository.save(entity);

        String response = mockMvc.perform(get("/api/apps/json-app/cases/json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Should be valid JSON array
        ObjectMapper mapper = new ObjectMapper();
        java.util.List<?> list = mapper.readValue(response, java.util.List.class);
        assert list.size() == 1;
    }

    @Test
    void testGetCasesForNonexistentApp() throws Exception {
        mockMvc.perform(get("/api/apps/no-such-app/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
