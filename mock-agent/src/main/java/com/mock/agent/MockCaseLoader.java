package com.mock.agent;

import com.mock.agent.api.ManagementApiServer;
import com.mock.agent.api.MockStatistics;
import com.mock.agent.loader.CaseSource;
import com.mock.agent.loader.CompositeCaseLoader;
import com.mock.agent.loader.FileCaseSource;
import com.mock.agent.loader.RemoteCaseSource;
import com.mock.agent.log.MockAgentLogger;
import com.mock.agent.match.CompositeMatchEngine;
import com.mock.agent.server.AgentCommunicator;
import com.mock.agent.store.MemoryCaseStore;

import java.util.ArrayList;
import java.util.List;

public class MockCaseLoader {

    private static final String DEFAULT_SERVER_URL = "http://localhost:8090";

    private static final CompositeCaseLoader CASE_LOADER;
    private static final MockCaseManager MANAGER;
    private static final MockStatistics STATISTICS = new MockStatistics();
    private static ManagementApiServer apiServer;
    private static AgentCommunicator communicator;
    private static volatile boolean initialized = false;
    private static volatile String resolvedAppName;

    static {
        List<CaseSource> sources = new ArrayList<>();
        sources.add(new FileCaseSource());
        // Remote source added lazily after app name is resolved

        CASE_LOADER = new CompositeCaseLoader(sources);
        MANAGER = new MockCaseManager(CASE_LOADER, new MemoryCaseStore(), new CompositeMatchEngine());
    }

    static String getResolvedAppName() {
        if (resolvedAppName == null) {
            resolvedAppName = AppNameDetector.detect();
        }
        return resolvedAppName;
    }

    /**
     * Called from premain to start management API and register agent as early as possible.
     * Runs in a background thread to avoid blocking application startup.
     */
    public static void earlyInit() {
        boolean autoRegister = !"false".equals(System.getProperty("mock.agent.autoRegister"));
        String apiPortStr = System.getProperty("mock.agent.api.port");

        if (!autoRegister && apiPortStr == null) {
            return;
        }

        final String portConfig = apiPortStr != null ? apiPortStr : "0";

        Thread earlyThread = new Thread(() -> {
            try {
                // Give the classloader a moment to settle
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }

            try {
                int port = "0".equals(portConfig) ? 0 : Integer.parseInt(portConfig);
                startManagementApi(port);
                System.err.println("[MockAgent] Early registration done, app: " + getResolvedAppName());
            } catch (NumberFormatException e) {
                MockAgentLogger.error("invalid mock.agent.api.port: " + portConfig);
            } catch (Exception e) {
                MockAgentLogger.warn("Early init failed, will retry on first mock call: " + e.getMessage());
            }
        }, "mock-agent-early-init");
        earlyThread.setDaemon(true);
        earlyThread.start();
    }

    private static synchronized void lazyInit() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Now that Spring Boot is running, resolve app name and add remote source
        String appName = getResolvedAppName();
        String serverUrl = System.getProperty("mock.agent.server.url", DEFAULT_SERVER_URL);
        CASE_LOADER.addSource(new RemoteCaseSource(serverUrl, appName));
        MockAgentLogger.info("Remote case source: " + serverUrl + " for app: " + appName);
        System.err.println("[MockAgent] Resolved app name: " + appName);

        // Start management API if not already started by earlyInit()
        if (apiServer == null) {
            boolean autoRegister = !"false".equals(System.getProperty("mock.agent.autoRegister"));
            String apiPortStr = System.getProperty("mock.agent.api.port");

            if (autoRegister && apiPortStr == null) {
                apiPortStr = "0";
            }

            if (apiPortStr != null) {
                try {
                    int port = "0".equals(apiPortStr) ? 0 : Integer.parseInt(apiPortStr);
                    startManagementApi(port);
                } catch (NumberFormatException e) {
                    MockAgentLogger.error("invalid mock.agent.api.port: " + apiPortStr);
                }
            }
        }
    }

    public static MockCase findMatch(String method, String path, String requestBody) {
        lazyInit();
        long startTime = System.currentTimeMillis();
        MockCase result = MANAGER.findMatch(method, path, requestBody);
        long matchTime = System.currentTimeMillis() - startTime;
        if (result != null) {
            STATISTICS.recordMatch(method + ":" + path, method, path, requestBody, matchTime, result.getStatus());
        } else {
            STATISTICS.recordPassThrough(method, path, requestBody, "no match");
        }
        return result;
    }

    public static void reload() {
        MANAGER.reload();
    }

    public static MockStatistics getStatistics() {
        return STATISTICS;
    }

    public static MockCaseManager getManager() {
        return MANAGER;
    }

    public static CompositeCaseLoader getCaseLoader() {
        return CASE_LOADER;
    }

    public static synchronized void startManagementApi(int port) {
        if (apiServer != null) {
            return;
        }
        try {
            apiServer = new ManagementApiServer(MANAGER, STATISTICS);
            apiServer.start(port);
            int actualPort = apiServer.getPort();
            MockAgentLogger.info("Management API started on port " + actualPort);

            // Start agent communication
            String serverUrl = System.getProperty("mock.agent.server.url", DEFAULT_SERVER_URL);
            communicator = new AgentCommunicator(serverUrl, getResolvedAppName(), actualPort);
            communicator.start();
        } catch (Exception e) {
            MockAgentLogger.error("Failed to start management API", e);
        }
    }

    public static synchronized void stopManagementApi() {
        if (communicator != null) {
            communicator.stop();
            communicator = null;
        }
        if (apiServer != null) {
            apiServer.stop();
            apiServer = null;
        }
    }

    public static AgentCommunicator getCommunicator() {
        return communicator;
    }
}
