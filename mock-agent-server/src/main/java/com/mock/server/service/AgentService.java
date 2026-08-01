package com.mock.server.service;

import com.mock.server.entity.AgentInstance;
import com.mock.server.repository.AgentInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AgentService {

    @Autowired
    private AgentInstanceRepository repository;

    public AgentInstance register(String instanceId, String appName, String ip, int port) {
        Optional<AgentInstance> existing = repository.findByInstanceId(instanceId);
        AgentInstance agent;
        if (existing.isPresent()) {
            agent = existing.get();
            agent.setAppName(appName);
            agent.setIp(ip);
            agent.setPort(port);
            agent.setOnline(true);
            agent.setLastHeartbeat(LocalDateTime.now());
        } else {
            agent = new AgentInstance();
            agent.setInstanceId(instanceId);
            agent.setAppName(appName);
            agent.setIp(ip);
            agent.setPort(port);
            agent.setOnline(true);
        }
        return repository.save(agent);
    }

    public void heartbeat(String instanceId) {
        repository.findByInstanceId(instanceId).ifPresent(agent -> {
            agent.setLastHeartbeat(LocalDateTime.now());
            agent.setOnline(true);
            repository.save(agent);
        });
    }

    public List<AgentInstance> findAll() {
        List<AgentInstance> all = repository.findAll();
        markStaleOffline(all);
        return repository.findAll();
    }

    public List<AgentInstance> findOnline() {
        // Mark stale agents as offline first
        List<AgentInstance> all = repository.findAll();
        markStaleOffline(all);
        return repository.findByOnlineTrue();
    }

    @Scheduled(fixedRate = 30000)
    public void scheduledStaleCheck() {
        markStaleOffline(repository.findAll());
    }

    private void markStaleOffline(List<AgentInstance> agents) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
        for (AgentInstance agent : agents) {
            if (agent.isOnline() && agent.getLastHeartbeat() != null
                    && agent.getLastHeartbeat().isBefore(threshold)) {
                agent.setOnline(false);
                repository.save(agent);
            }
        }
    }

    public List<AgentInstance> findByAppName(String appName) {
        return repository.findByAppName(appName);
    }

    public void markOffline(String instanceId) {
        repository.findByInstanceId(instanceId).ifPresent(agent -> {
            agent.setOnline(false);
            repository.save(agent);
        });
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
