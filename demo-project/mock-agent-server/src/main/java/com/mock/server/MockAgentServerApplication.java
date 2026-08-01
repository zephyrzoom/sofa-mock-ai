package com.mock.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MockAgentServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockAgentServerApplication.class, args);
    }
}
