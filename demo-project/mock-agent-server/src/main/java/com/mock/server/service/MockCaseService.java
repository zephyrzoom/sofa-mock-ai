package com.mock.server.service;

import com.mock.server.entity.MockCaseEntity;
import com.mock.server.repository.MockCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MockCaseService {

    @Autowired
    private MockCaseRepository repository;

    public List<MockCaseEntity> findAll() {
        return repository.findAll();
    }

    public List<MockCaseEntity> findByAppName(String appName) {
        return repository.findByAppName(appName);
    }

    public List<MockCaseEntity> findEnabledByAppName(String appName) {
        return repository.findByAppNameAndEnabledTrue(appName);
    }

    public Optional<MockCaseEntity> findById(Long id) {
        return repository.findById(id);
    }

    public MockCaseEntity save(MockCaseEntity entity) {
        return repository.save(entity);
    }

    public List<MockCaseEntity> saveAll(List<MockCaseEntity> entities) {
        return repository.saveAll(entities);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return repository.existsById(id);
    }
}
