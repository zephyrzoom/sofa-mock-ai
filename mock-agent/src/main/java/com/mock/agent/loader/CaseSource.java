package com.mock.agent.loader;

import java.util.List;

public interface CaseSource {

    List<CaseLoader.LoadedCase> loadCases();

    boolean supportsHotReload();

    default void addChangeListener(Runnable listener) {
        // default no-op
    }
}
