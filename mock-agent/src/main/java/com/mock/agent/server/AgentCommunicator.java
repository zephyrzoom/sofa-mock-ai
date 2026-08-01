package com.mock.agent.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.agent.log.MockAgentLogger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentCommunicator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String serverUrl;
    private final String appName;
    private final String instanceId;
    private final String ip;
    private final int managementPort;
    private ScheduledExecutorService scheduler;
    private volatile boolean registered = false;

    public AgentCommunicator(String serverUrl, String appName, int managementPort) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.appName = appName;
        this.instanceId = appName + "-" + UUID.randomUUID().toString().substring(0, 8);
        this.ip = detectIp();
        this.managementPort = managementPort;
    }

    public void start() {
        register();
        startHeartbeat();
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        markOffline();
    }

    private void register() {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("instanceId", instanceId);
            body.put("appName", appName);
            body.put("ip", ip);
            body.put("port", managementPort);

            MockAgentLogger.info("Registering agent: " + instanceId + " to " + serverUrl);
            String response = post(serverUrl + "/api/agents/register", body);
            if (response != null) {
                registered = true;
                MockAgentLogger.info("Agent registered: " + instanceId + " at " + ip + ":" + managementPort);
            } else {
                MockAgentLogger.warn("Agent registration failed: server returned non-2xx");
            }
        } catch (Exception e) {
            MockAgentLogger.warn("Failed to register agent: " + e.getMessage());
        }
    }

    private void startHeartbeat() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mock-agent-heartbeat");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // If not registered yet, retry registration
                if (!registered) {
                    register();
                    return;
                }
                Map<String, String> body = new HashMap<>();
                body.put("instanceId", instanceId);
                post(serverUrl + "/api/agents/heartbeat", body);
            } catch (Exception e) {
                MockAgentLogger.debug("Heartbeat failed: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private void markOffline() {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("instanceId", instanceId);
            post(serverUrl + "/api/agents/" + instanceId + "/offline", body);
        } catch (Exception e) {
            // best effort
        }
    }

    private String post(String urlStr, Object body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setDoOutput(true);

        byte[] json = MAPPER.writeValueAsBytes(body);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json);
        }

        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            try (InputStream is = conn.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                return new String(bos.toByteArray(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String detectIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public String getInstanceId() {
        return instanceId;
    }

    public boolean isRegistered() {
        return registered;
    }
}
