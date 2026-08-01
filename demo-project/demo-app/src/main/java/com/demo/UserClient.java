package com.demo;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "user-service",
        url = "http://127.0.0.1:9999"
)
public interface UserClient {

    @PostMapping("/user/query")
    String queryUser(@RequestBody String body);
}
