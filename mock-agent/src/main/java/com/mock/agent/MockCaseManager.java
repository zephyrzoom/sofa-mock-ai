package com.mock.agent;

import com.mock.agent.loader.CaseLoader;
import com.mock.agent.match.MatchEngine;
import com.mock.agent.store.CaseStore;
import com.mock.agent.log.MockAgentLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class MockCaseManager {

    private final CaseLoader caseLoader;
    private final CaseStore caseStore;
    private final MatchEngine matchEngine;
    private final Map<String, Long> fileTimestamps = new HashMap<>();
    private final Map<String, Set<String>> fileKeys = new HashMap<>();

    public MockCaseManager(CaseLoader caseLoader, CaseStore caseStore, MatchEngine matchEngine) {
        this.caseLoader = caseLoader;
        this.caseStore = caseStore;
        this.matchEngine = matchEngine;
    }

    public synchronized void load() {
        List<CaseLoader.LoadedCase> loadedCases = caseLoader.loadAll();
        Set<String> currentFileNames = new HashSet<>();

        for (CaseLoader.LoadedCase lc : loadedCases) {
            String fileName = lc.getFileName();
            currentFileNames.add(fileName);

            String k = key(lc.getMethod(), lc.getPath());

            // Remove only the cases this file contributed on the previous load.
            // Do NOT clear the whole key: the same method+path may also have cases
            // from other sources (e.g. local file + admin-uploaded remote case).
            Set<String> oldKeys = fileKeys.remove(fileName);
            if (oldKeys != null) {
                for (String oldKey : oldKeys) {
                    String[] parts = oldKey.split(":", 2);
                    List<MockCase> cases = caseStore.findByKey(parts[0], parts[1]);
                    if (cases != null) {
                        cases.removeIf(mc -> fileName.equals(mc.getSource()));
                        if (cases.isEmpty()) {
                            caseStore.remove(parts[0], parts[1]);
                        }
                    }
                }
            }

            MockCase mc = new MockCase();
            mc.setMethod(lc.getMethod());
            mc.setPath(lc.getPath());
            mc.setRequestBody(lc.getRequestBody());
            mc.setCondition(lc.getCondition());
            mc.setStatus(lc.getStatus());
            mc.setBody(lc.getBody());
            mc.setPathPattern(lc.getPathPattern());
            mc.setMatchType(lc.getMatchType());
            if (lc.getRequestHeaders() != null) {
                mc.setRequestHeaders(lc.getRequestHeaders());
            }
            if (lc.getResponseHeaders() != null) {
                mc.setResponseHeaders(lc.getResponseHeaders());
            }
            mc.setDelayMs(lc.getDelayMs());
            mc.setDescription(lc.getDescription());
            mc.setEnabled(lc.isEnabled());
            mc.setPriority(lc.getPriority());
            mc.setSource(fileName);

            List<MockCase> cases = caseStore.findByKey(lc.getMethod(), lc.getPath());
            if (cases == null) {
                cases = new ArrayList<>();
                caseStore.put(lc.getMethod(), lc.getPath(), cases);
            }
            cases.add(mc);

            Set<String> keys = new HashSet<>();
            keys.add(k);
            fileKeys.put(fileName, keys);
            MockAgentLogger.info("loaded mock case: " + lc.getMethod() + " " + lc.getPath() + " from " + fileName);
        }

        // Clean up deleted files
        for (Map.Entry<String, Set<String>> entry : new HashMap<>(fileKeys).entrySet()) {
            String fileName = entry.getKey();
            if (!currentFileNames.contains(fileName)) {
                Set<String> removedKeys = fileKeys.remove(fileName);
                if (removedKeys != null) {
                    for (String k : removedKeys) {
                        String[] parts = k.split(":", 2);
                        List<MockCase> cases = caseStore.findByKey(parts[0], parts[1]);
                        if (cases != null) {
                            cases.removeIf(mc -> fileName.equals(mc.getSource()));
                            if (cases.isEmpty()) {
                                caseStore.remove(parts[0], parts[1]);
                            }
                        }
                    }
                }
                fileTimestamps.remove(fileName);
                MockAgentLogger.info("removed mock case(s) from deleted file: " + fileName);
            }
        }
    }

    public MockCase findMatch(String method, String path, String requestBody) {
        load();
        List<MockCase> candidates = caseStore.findByKey(method, path);
        return matchEngine.findMatch(method, path, requestBody, candidates);
    }

    public void reload() {
        fileTimestamps.clear();
        caseStore.clear();
        fileKeys.clear();
        load();
    }

    public List<MockCase> getAllCases() {
        return caseStore.findAll();
    }

    private static String key(String method, String path) {
        return method.toUpperCase() + ":" + path;
    }
}
