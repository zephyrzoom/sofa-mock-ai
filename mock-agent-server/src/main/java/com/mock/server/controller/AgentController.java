package com.mock.server.controller;

import com.mock.server.entity.AgentInstance;
import com.mock.server.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    public AgentInstance register(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String instanceId = (String) request.get("instanceId");
        String appName = (String) request.get("appName");
        int port = request.get("port") != null ? ((Number) request.get("port")).intValue() : 0;
        // 用注册请求的源 IP 作为 agent 可达地址：agent 能连到本服务，源 IP 必然可达，
        // 避免 agent 自报的 127.0.0.1 / 虚拟网卡地址导致管理端无法回连（监控/统计 502）。
        String ip = httpRequest.getRemoteAddr();
        return service.register(instanceId, appName, ip, port);
    }

    @PostMapping("/heartbeat")
    public Map<String, String> heartbeat(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String instanceId = request.get("instanceId");
        // 心跳时用源 IP 刷新地址，应对 DHCP 等地址变化
        service.heartbeat(instanceId, httpRequest.getRemoteAddr());
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

    /**
     * 代理 agent 内置管理 API 的事件列表，供控制台内网跨机访问。
     * 浏览器不再直连 agent（跨域/不可达），统一由本服务中转。
     */
    @GetMapping(value = "/{id}/events", produces = "application/json; charset=utf-8")
    public String proxyEvents(@PathVariable Long id) {
        return fetchFromAgent(id, "/mock/events");
    }

    /** 代理 agent 内置管理 API 的统计信息。 */
    @GetMapping(value = "/{id}/stats", produces = "application/json; charset=utf-8")
    public String proxyStats(@PathVariable Long id) {
        return fetchFromAgent(id, "/mock/stats");
    }

    private String fetchFromAgent(Long id, String path) {
        AgentInstance agent = service.getById(id);
        if (agent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "agent not found");
        }
        try {
            URL url = new URL("http://" + agent.getIp() + ":" + agent.getPort() + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (InputStream is = conn.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        bos.write(buf, 0, len);
                    }
                    return bos.toString(StandardCharsets.UTF_8.name());
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "agent returned " + code);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "cannot reach agent: " + e.getMessage());
        }
    }
}
