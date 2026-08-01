package com.mock.agent.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class MockStatistics {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong matchedRequests = new AtomicLong(0);
    private final AtomicLong passThroughRequests = new AtomicLong(0);
    private final Map<String, AtomicLong> caseMatchCounts = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<MatchEvent> recentEvents = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<MatchEvent>> eventListeners = new CopyOnWriteArrayList<>();
    private static final int MAX_RECENT_EVENTS = 100;

    public void recordMatch(String caseKey, String method, String path, String requestBody,
                            long matchTimeMs, int responseStatus) {
        totalRequests.incrementAndGet();
        matchedRequests.incrementAndGet();
        caseMatchCounts.computeIfAbsent(caseKey, k -> new AtomicLong(0)).incrementAndGet();

        String caseId = caseKey;
        MatchEvent event = new MatchEvent(
                System.currentTimeMillis(), method, path,
                truncate(requestBody, 100),
                true, caseId, matchTimeMs, responseStatus, null
        );
        addEvent(event);
    }

    public void recordPassThrough(String method, String path, String requestBody, String reason) {
        totalRequests.incrementAndGet();
        passThroughRequests.incrementAndGet();

        MatchEvent event = new MatchEvent(
                System.currentTimeMillis(), method, path,
                truncate(requestBody, 100),
                false, null, 0, 0, reason
        );
        addEvent(event);
    }

    public void addEventListener(Consumer<MatchEvent> listener) {
        eventListeners.add(listener);
    }

    public void removeEventListener(Consumer<MatchEvent> listener) {
        eventListeners.remove(listener);
    }

    public List<MatchEvent> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalRequests", totalRequests.get());
        stats.put("matchedRequests", matchedRequests.get());
        stats.put("passThroughRequests", passThroughRequests.get());
        stats.put("matchRate", totalRequests.get() > 0
                ? Math.round((double) matchedRequests.get() / totalRequests.get() * 100.0) / 100.0
                : 0.0);
        stats.put("recentEventsCount", recentEvents.size());
        return stats;
    }

    public long getTotalRequests() { return totalRequests.get(); }
    public long getMatchedRequests() { return matchedRequests.get(); }
    public long getPassThroughRequests() { return passThroughRequests.get(); }

    private void addEvent(MatchEvent event) {
        recentEvents.add(0, event);
        while (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.remove(recentEvents.size() - 1);
        }
        for (Consumer<MatchEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                // ignore listener errors
            }
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
