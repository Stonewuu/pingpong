package com.challenge.ping.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import com.challenge.ping.service.PingService;
import com.challenge.ping.service.GlobalRateLimiter;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;

@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "test")
    public PingService testPingService(WebClient.Builder webClientBuilder) throws Exception {
        return new PingService(webClientBuilder, new GlobalRateLimiter(2, Duration.ofSeconds(1))) {
            @Override
            public void schedulePeriodicPing() {
                // 测试环境下不启动定时任务
            }
        };
    }
} 