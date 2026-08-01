package com.mock.agent.loader;

import com.mock.agent.log.MockAgentLogger;

import java.util.ArrayList;
import java.util.List;

public class CompositeCaseLoader implements CaseLoader {

    private final List<CaseSource> sources;

    public CompositeCaseLoader(List<CaseSource> sources) {
        this.sources = new ArrayList<>(sources);
    }

    @Override
    public List<LoadedCase> loadAll() {
        List<LoadedCase> allCases = new ArrayList<>();

        for (CaseSource source : sources) {
            try {
                List<LoadedCase> cases = source.loadCases();
                allCases.addAll(cases);
            } catch (Exception e) {
                MockAgentLogger.error("Failed to load from source: " + source.getClass().getSimpleName(), e);
            }
        }

        return allCases;
    }

    public void addSource(CaseSource source) {
        sources.add(source);
    }
}
