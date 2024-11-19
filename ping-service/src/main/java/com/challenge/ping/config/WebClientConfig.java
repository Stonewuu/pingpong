package com.challenge.ping.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Value("${spring.application.instance-id}")
    private String instanceId;
    
    @Bean
    public String instanceId() {
        return instanceId;
    }
} 