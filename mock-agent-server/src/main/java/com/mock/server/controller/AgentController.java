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
                    return repairJsonStrings(bos.toString(StandardCharsets.UTF_8.name()));
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "agent returned " + code);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "cannot reach agent: " + e.getMessage());
        }
    }

    /**
     * 旧版 agent 的手写 JSON 转义只处理了反斜杠和引号，请求体含换行/制表符等控制字符时
     * 会产出非法 JSON（字符串值内出现真实换行符）。这里把字符串值内的控制字符转义，
     * 保证返回给前端的 JSON 始终可解析。对合法 JSON 是无操作。
     */
    private static String repairJsonStrings(String json) {
        if (json == null) return null;
        StringBuilder out = new StringBuilder(json.length() + 64);
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                out.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                out.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                out.append(c);
                inString = !inString;
                continue;
            }
            if (inString && c < 0x20) {
                switch (c) {
                    case '\n': out.append("\\n"); break;
                    case '\r': out.append("\\r"); break;
                    case '\t': out.append("\\t"); break;
                    default: out.append(String.format("\\u%04x", (int) c));
                }
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
