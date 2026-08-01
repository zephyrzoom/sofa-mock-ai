package com.mock.server.controller;

import com.mock.server.entity.AgentInstance;
import com.mock.server.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    @Autowired
    private AgentService service;

    @GetMapping
    public List<AgentInstance> list(@RequestParam(required = false) String appName) {
        if (appName != null && !appName.isEmpty()) {
            return service.findByAppName(appName);
        }
        return service.findAll();
    }

    @GetMapping("/online")
    public List<AgentInstance> listOnline() {
        return service.findOnline();
    }

    @PostMapping("/register")
    public AgentInstance register(@RequestBody Map<String, Object> request) {
        String instanceId = (String) request.get("instanceId");
        String appName = (String) request.get("appName");
        String ip = (String) request.get("ip");
        int port = request.get("port") != null ? ((Number) request.get("port")).intValue() : 0;
        return service.register(instanceId, appName, ip, port);
    }

    @PostMapping("/heartbeat")
    public Map<String, String> heartbeat(@RequestBody Map<String, String> request) {
        String instanceId = request.get("instanceId");
        service.heartbeat(instanceId);
        return java.util.Collections.singletonMap("status", "ok");
    }

    @PostMapping("/{instanceId}/offline")
    public Map<String, String> markOffline(@PathVariable String instanceId) {
        service.markOffline(instanceId);
        return java.util.Collections.singletonMap("status", "ok");
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteById(id);
    }
}
