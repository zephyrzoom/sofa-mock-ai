package com.mock.server.controller;

import com.mock.server.entity.MockCaseEntity;
import com.mock.server.service.MockCaseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cases")
public class MockCaseController {

    @Autowired
    private MockCaseService service;

    @GetMapping
    public List<MockCaseEntity> list(@RequestParam(required = false) String appName) {
        if (appName != null && !appName.isEmpty()) {
            return service.findByAppName(appName);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MockCaseEntity> get(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public MockCaseEntity create(@RequestBody MockCaseEntity entity) {
        return service.save(entity);
    }

    @PostMapping("/batch")
    public List<MockCaseEntity> createBatch(@RequestBody List<MockCaseEntity> entities) {
        return service.saveAll(entities);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MockCaseEntity> update(@PathVariable Long id, @RequestBody MockCaseEntity entity) {
        if (!service.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        entity.setId(id);
        return ResponseEntity.ok(service.save(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!service.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        service.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export")
    public List<MockCaseEntity> export(@RequestParam(required = false) String appName) {
        if (appName != null && !appName.isEmpty()) {
            return service.findByAppName(appName);
        }
        return service.findAll();
    }

    @GetMapping("/apps")
    public List<String> listApps() {
        return service.findAll().stream()
                .map(MockCaseEntity::getAppName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> jsonList = mapper.readValue(
                    file.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});

            List<MockCaseEntity> entities = new ArrayList<>();
            for (Map<String, Object> json : jsonList) {
                MockCaseEntity entity = new MockCaseEntity();
                entity.setAppName((String) json.getOrDefault("appName", "default"));
                entity.setMethod((String) json.get("method"));
                entity.setPath((String) json.get("path"));
                entity.setPathPattern((String) json.get("pathPattern"));
                entity.setMatchType((String) json.get("matchType"));
                entity.setRequestBody((String) json.get("requestBody"));
                entity.setCondition((String) json.get("condition"));

                Object statusObj = json.get("status");
                entity.setStatus(statusObj instanceof Number ? ((Number) statusObj).intValue() : 200);

                entity.setBody((String) json.get("body"));
                entity.setDescription((String) json.get("description"));

                Object enabledObj = json.get("enabled");
                entity.setEnabled(enabledObj == null || (Boolean) enabledObj);

                Object priorityObj = json.get("priority");
                entity.setPriority(priorityObj instanceof Number ? ((Number) priorityObj).intValue() : 0);

                Object delayObj = json.get("delayMs");
                entity.setDelayMs(delayObj instanceof Number ? ((Number) delayObj).longValue() : 0L);

                entities.add(entity);
            }

            List<MockCaseEntity> saved = service.saveAll(entities);
            Map<String, Object> result = new HashMap<>();
            result.put("imported", saved.size());
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", e.getMessage());
            result.put("imported", 0);
            return result;
        }
    }
}
