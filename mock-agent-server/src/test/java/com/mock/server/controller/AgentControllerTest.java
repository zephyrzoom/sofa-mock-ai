package com.mock.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.server.entity.AgentInstance;
import com.mock.server.repository.AgentInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentInstanceRepository repository;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testRegisterAgent() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("instanceId", "test-instance-1");
        request.put("appName", "test-app");
        request.put("ip", "192.168.1.100");
        request.put("port", 9090);

        mockMvc.perform(post("/api/agents/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceId").value("test-instance-1"))
                .andExpect(jsonPath("$.appName").value("test-app"))
                .andExpect(jsonPath("$.online").value(true));
    }

    @Test
    void testHeartbeat() throws Exception {
        AgentInstance agent = new AgentInstance();
        agent.setInstanceId("hb-instance");
        agent.setAppName("hb-app");
        agent.setIp("10.0.0.1");
        agent.setPort(9090);
        agent.setOnline(true);
        repository.save(agent);

        Map<String, String> request = new HashMap<>();
        request.put("instanceId", "hb-instance");

        mockMvc.perform(post("/api/agents/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void testListAgents() throws Exception {
        AgentInstance a1 = new AgentInstance();
        a1.setInstanceId("list-1");
        a1.setAppName("list-app");
        a1.setIp("10.0.0.1");
        a1.setPort(9090);
        a1.setOnline(true);
        repository.save(a1);

        AgentInstance a2 = new AgentInstance();
        a2.setInstanceId("list-2");
        a2.setAppName("list-app");
        a2.setIp("10.0.0.2");
        a2.setPort(9090);
        a2.setOnline(true);
        repository.save(a2);

        mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testDeleteAgent() throws Exception {
        AgentInstance agent = new AgentInstance();
        agent.setInstanceId("del-instance");
        agent.setAppName("del-app");
        agent.setIp("10.0.0.1");
        agent.setPort(9090);
        agent.setOnline(true);
        agent = repository.save(agent);

        mockMvc.perform(delete("/api/agents/" + agent.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testListOnlineAgents() throws Exception {
        AgentInstance online = new AgentInstance();
        online.setInstanceId("online-1");
        online.setAppName("app");
        online.setIp("10.0.0.1");
        online.setPort(9090);
        online.setOnline(true);
        repository.save(online);

        AgentInstance offline = new AgentInstance();
        offline.setInstanceId("offline-1");
        offline.setAppName("app");
        offline.setIp("10.0.0.2");
        offline.setPort(9090);
        offline.setOnline(false);
        repository.save(offline);

        mockMvc.perform(get("/api/agents/online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].instanceId").value("online-1"));
    }
}
