package com.mock.server.repository;

import com.mock.server.entity.MockCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockCaseRepository extends JpaRepository<MockCaseEntity, Long> {

    List<MockCaseEntity> findByAppName(String appName);

    List<MockCaseEntity> findByAppNameAndEnabledTrue(String appName);

    List<MockCaseEntity> findByMethodAndPath(String method, String path);
}
