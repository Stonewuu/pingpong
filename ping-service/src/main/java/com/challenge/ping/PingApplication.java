package com.challenge.ping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.challenge")
@EnableScheduling
public class PingApplication {
    public static void main(String[] args) {
        SpringApplication.run(PingApplication.class, args);
    }
} 