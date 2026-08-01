package com.demo;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "user-service",
        url = "http://127.0.0.1:9999"
)
public interface UserClient {

    @PostMapping("/user/query")
    String queryUser(@RequestBody Map<String, Object> req);
}
