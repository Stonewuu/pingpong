package com.challenge.pong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.challenge")
@EntityScan(basePackages = "com.challenge.model")
public class PongApplication {
    public static void main(String[] args) {
        SpringApplication.run(PongApplication.class, args);
    }
} 