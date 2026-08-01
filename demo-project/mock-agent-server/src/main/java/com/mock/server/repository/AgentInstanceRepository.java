package com.mock.server.repository;

import com.mock.server.entity.AgentInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentInstanceRepository extends JpaRepository<AgentInstance, Long> {

    Optional<AgentInstance> findByInstanceId(String instanceId);

    List<AgentInstance> findByAppName(String appName);

    List<AgentInstance> findByOnlineTrue();
}
