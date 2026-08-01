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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ManagementApiServer {

    /** Heartbeat interval for SSE keep-alive comments (ms). */
    private static final long SSE_HEARTBEAT_MS = 10000;

    private final MockCaseManager caseManager;
    private final MockStatistics statistics;
    private HttpServer server;

    public ManagementApiServer(MockCaseManager caseManager, MockStatistics statistics) {
        this.caseManager = caseManager;
        this.statistics = statistics;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        // Cached pool so long-lived SSE connections do not starve other endpoints.
        server.setExecutor(Executors.newCachedThreadPool());

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

    /**
     * Answers CORS preflight and marks the response with CORS headers so the
     * management console (a different origin than the agent) can call this API
     * directly from the browser. Returns true if the request was an OPTIONS
     * preflight that has already been answered.
     */
    private boolean handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
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
            if (handleCors(exchange)) return;
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
            if (handleCors(exchange)) return;
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
            if (handleCors(exchange)) return;
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
            if (handleCors(exchange)) return;
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
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            AtomicBoolean closed = new AtomicBoolean(false);

            // Use an array to hold the listener so it can remove itself on failure.
            final Consumer<MatchEvent>[] listenerRef = new Consumer[1];
            Consumer<MatchEvent> listener = event -> {
                if (closed.get()) {
                    return;
                }
                try {
                    os.write(("data: " + event.toJson() + "\n\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                } catch (IOException e) {
                    closed.set(true);
                    statistics.removeEventListener(listenerRef[0]);
                }
            };
            listenerRef[0] = listener;
            statistics.addEventListener(listener);

            try {
                // Flush response headers and verify the socket is writable.
                os.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();

                // Keep the stream open. SSE comments are ignored by EventSource,
                // so heartbeats only serve to detect a closed client connection:
                // writing to a dead socket throws IOException, letting us clean up
                // and release the worker thread instead of holding it forever.
                while (!closed.get()) {
                    Thread.sleep(SSE_HEARTBEAT_MS);
                    os.write(": heartbeat\n\n".getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (IOException | InterruptedException e) {
                // Client disconnected or the handler thread was interrupted.
            } finally {
                closed.set(true);
                statistics.removeEventListener(listener);
            }
        }
    }
}
