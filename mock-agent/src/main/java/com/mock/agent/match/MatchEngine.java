package com.mock.agent.match;

import com.mock.agent.MockCase;

import java.util.List;

public interface MatchEngine {

    MockCase findMatch(String method, String path, String requestBody, List<MockCase> candidates);
}
