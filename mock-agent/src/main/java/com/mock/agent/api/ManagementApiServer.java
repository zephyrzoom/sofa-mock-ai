package com.mock.agent.api;

import com.mock.agent.MockCase;
import com.mock.agent.MockCaseManager;
import com.mock.agent.log.MockAgentLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ManagementApiServer {

    private final MockCaseManager caseManager;
    private final MockStatistics statistics;
    private HttpServer server;

    public ManagementApiServer(MockCaseManager caseManager, MockStatistics statistics) {
        this.caseManager = caseManager;
        this.statistics = statistics;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        server.createContext("/mock/cases", new CasesHandler());
        server.createContext("/mock/status", new StatusHandler());
        server.createContext("/mock/stats", new StatsHandler());
        server.createContext("/mock/events", new EventsHandler());

        server.start();
        MockAgentLogger.info("Management API started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            MockAgentLogger.info("Management API stopped");
        }
    }

    public int getPort() {
        return server != null ? server.getAddress().getPort() : 0;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private class CasesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && "/mock/cases".equals(path)) {
                handleListCases(exchange);
            } else if ("POST".equals(method) && "/mock/cases/reload".equals(path)) {
                handleReload(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"not found\"}");
            }
        }

        private void handleListCases(HttpExchange exchange) throws IOException {
            caseManager.load();
            List<MockCase> allCases = caseManager.getAllCases();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < allCases.size(); i++) {
                if (i > 0) sb.append(",");
                MockCase mc = allCases.get(i);
                sb.append("{\"method\":\"").append(mc.getMethod())
                  .append("\",\"path\":\"").append(mc.getPath())
                  .append("\",\"status\":").append(mc.getStatus())
                  .append(",\"enabled\":").append(mc.isEnabled())
                  .append("}");
            }
            sb.append("]");
            sendResponse(exchange, 200, sb.toString());
        }

        private void handleReload(HttpExchange exchange) throws IOException {
            caseManager.reload();
            sendResponse(exchange, 200, "{\"message\":\"reloaded\"}");
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            sendResponse(exchange, 200, "{\"status\":\"running\",\"agent\":\"mock-agent\"}");
        }
    }

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            Map<String, Object> stats = statistics.getStats();
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                Object val = entry.getValue();
                if (val instanceof String) {
                    sb.append("\"").append(val).append("\"");
                } else {
                    sb.append(val);
                }
                first = false;
            }
            sb.append("}");
            sendResponse(exchange, 200, sb.toString());
        }
    }

    private class EventsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if ("/mock/events/stream".equals(path)) {
                handleSseStream(exchange);
            } else if ("/mock/events".equals(path)) {
                handleListEvents(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"not found\"}");
            }
        }

        private void handleListEvents(HttpExchange exchange) throws IOException {
            List<MatchEvent> events = statistics.getRecentEvents();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < events.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(events.get(i).toJson());
            }
            sb.append("]");
            sendResponse(exchange, 200, sb.toString());
        }

        private void handleSseStream(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            statistics.addEventListener(event -> {
                try {
                    String sseData = "data: " + event.toJson() + "\n\n";
                    os.write(sseData.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                } catch (IOException e) {
                    statistics.removeEventListener(this::sendSseEvent);
                }
            });

            // Keep connection open
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void sendSseEvent(MatchEvent event) {
            // placeholder for lambda reference
        }
    }
}
