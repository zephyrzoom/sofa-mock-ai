package com.mock.agent.store;

import com.mock.agent.MockCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MemoryCaseStore implements CaseStore {

    private final ConcurrentMap<String, List<MockCase>> store = new ConcurrentHashMap<>();

    @Override
    public List<MockCase> findByKey(String method, String path) {
        return store.get(key(method, path));
    }

    @Override
    public void put(String method, String path, List<MockCase> cases) {
        store.put(key(method, path), cases);
    }

    @Override
    public void remove(String method, String path) {
        store.remove(key(method, path));
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public List<MockCase> findAll() {
        List<MockCase> all = new ArrayList<>();
        for (List<MockCase> cases : store.values()) {
            all.addAll(cases);
        }
        return all;
    }

    private static String key(String method, String path) {
        return method.toUpperCase() + ":" + path;
    }
}
