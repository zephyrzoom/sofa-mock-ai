package com.mock.agent.store;

import com.mock.agent.MockCase;

import java.util.List;

public interface CaseStore {

    List<MockCase> findByKey(String method, String path);

    List<MockCase> findAll();

    void put(String method, String path, List<MockCase> cases);

    void remove(String method, String path);

    void clear();
}
