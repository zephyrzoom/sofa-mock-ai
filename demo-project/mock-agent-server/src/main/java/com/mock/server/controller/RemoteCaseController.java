package com.mock.server.controller;

import com.mock.server.entity.MockCaseEntity;
import com.mock.server.service.MockCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/apps")
public class RemoteCaseController {

    @Autowired
    private MockCaseService service;

    @GetMapping("/{appName}/cases")
    public List<MockCaseEntity> getCasesForApp(@PathVariable String appName) {
        return service.findEnabledByAppName(appName);
    }

    @GetMapping("/{appName}/cases/json")
    public String getCasesAsJson(@PathVariable String appName) {
        List<MockCaseEntity> cases = service.findEnabledByAppName(appName);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cases.size(); i++) {
            if (i > 0) sb.append(",");
            MockCaseEntity mc = cases.get(i);
            sb.append("{");
            sb.append("\"method\":\"").append(mc.getMethod()).append("\"");
            sb.append(",\"path\":\"").append(mc.getPath()).append("\"");
            if (mc.getRequestBody() != null) {
                sb.append(",\"requestBody\":\"").append(escapeJson(mc.getRequestBody())).append("\"");
            }
            sb.append(",\"response\":{");
            sb.append("\"status\":").append(mc.getStatus());
            sb.append(",\"body\":\"").append(escapeJson(mc.getBody())).append("\"");
            sb.append("}");
            if (mc.getPathPattern() != null) {
                sb.append(",\"pathPattern\":\"").append(escapeJson(mc.getPathPattern())).append("\"");
            }
            if (mc.getMatchType() != null) {
                sb.append(",\"matchType\":\"").append(mc.getMatchType()).append("\"");
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
