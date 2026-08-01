package com.mock.agent;

import com.mock.agent.loader.CaseLoader;
import com.mock.agent.loader.CaseSource;
import com.mock.agent.loader.CompositeCaseLoader;
import com.mock.agent.match.CompositeMatchEngine;
import com.mock.agent.store.MemoryCaseStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockCaseManagerTest {

    /** A source whose loaded cases can be emptied to simulate a source disappearing. */
    private static class StubCaseSource implements CaseSource {
        private final String name;
        private final List<CaseLoader.LoadedCase> cases = new ArrayList<>();

        StubCaseSource(String name, List<CaseLoader.LoadedCase> cases) {
            this.name = name;
            this.cases.addAll(cases);
        }

        @Override
        public List<CaseLoader.LoadedCase> loadCases() {
            return new ArrayList<>(cases);
        }

        @Override
        public boolean supportsHotReload() {
            return true;
        }
    }

    @Test
    void shouldKeepLocalAndRemoteCasesForSameEndpoint() {
        // The same method+path served by both a local file case and a remote admin case,
        // distinguished only by their request body conditions.
        CaseLoader.LoadedCase local = new CaseLoader.LoadedCase(
                "local.json", "POST", "/api/test", "{\"local\":true}", 200, "{\"who\":\"local\"}");
        CaseLoader.LoadedCase remote = new CaseLoader.LoadedCase(
                "remote:app:POST:/api/test", "POST", "/api/test", "{\"remote\":true}", 200, "{\"who\":\"remote\"}");

        StubCaseSource localSource = new StubCaseSource("local", Arrays.asList(local));
        StubCaseSource remoteSource = new StubCaseSource("remote", Arrays.asList(remote));

        CompositeCaseLoader loader = new CompositeCaseLoader(Arrays.asList(localSource, remoteSource));
        MockCaseManager manager = new MockCaseManager(loader, new MemoryCaseStore(), new CompositeMatchEngine());

        // findMatch() triggers a reload every call; after several rounds both cases must survive.
        for (int i = 0; i < 3; i++) {
            MockCase matched = manager.findMatch("POST", "/api/test", "{\"remote\":true}");
            assertThat(matched).isNotNull();
            assertThat(matched.getBody()).isEqualTo("{\"who\":\"remote\"}");
        }
        for (int i = 0; i < 3; i++) {
            MockCase matched = manager.findMatch("POST", "/api/test", "{\"local\":true}");
            assertThat(matched).isNotNull();
            assertThat(matched.getBody()).isEqualTo("{\"who\":\"local\"}");
        }

        // Removing the remote source (e.g. admin deletes the case) must not remove the local case.
        remoteSource.cases.clear();
        MockCase localOnly = manager.findMatch("POST", "/api/test", "{\"local\":true}");
        assertThat(localOnly).isNotNull();
        assertThat(localOnly.getBody()).isEqualTo("{\"who\":\"local\"}");
        assertThat(manager.findMatch("POST", "/api/test", "{\"remote\":true}")).isNull();
    }
}
