package com.mock.agent.loader;

import java.util.List;

public class FileCaseSource implements CaseSource {

    private final JsonFileCaseLoader fileLoader;

    public FileCaseSource() {
        this.fileLoader = new JsonFileCaseLoader();
    }

    public FileCaseSource(String casesDirPath) {
        this.fileLoader = new JsonFileCaseLoader(casesDirPath);
    }

    @Override
    public List<CaseLoader.LoadedCase> loadCases() {
        return fileLoader.loadAll();
    }

    @Override
    public boolean supportsHotReload() {
        return true;
    }
}
