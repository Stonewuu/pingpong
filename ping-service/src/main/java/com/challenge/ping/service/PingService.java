package com.challenge.ping.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.challenge.ping.model.PingRecord;

import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import java.util.UUID;

@Service
@Slf4j
public class PingService {
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public enum PingStatus {
        SUCCESS("Pong responded successfully"),
        RATE_LIMITED_REMOTE("Rate limited by Pong service"),
        RATE_LIMITED_LOCAL("Rate limited by Ping service");
        
        private final String description;
        
        PingStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final record PingResult(PingStatus status, String response, String timestamp) {
        PingResult(PingStatus status, String response) {
            this(status, response, LocalDateTime.now().format(formatter));
        }
        
        @Override
        public String toString() {
            return String.format("[Status: %s, Response: %s, Time: %s]", 
                status.name(), response, timestamp);
        }
    }
    
    private final WebClient webClient;
    private final GlobalRateLimiter globalRateLimiter;
    private final PingMessageService messageService;
    
    @Value("${ping.pong-service.url}")
    private String pongServiceUrl;
    
    public PingService(
            WebClient.Builder webClientBuilder, 
            GlobalRateLimiter globalRateLimiter,
            @Value("${ping.pong-service.url}") String pongServiceUrl,
            PingMessageService messageService) {
        this.globalRateLimiter = globalRateLimiter;
        this.webClient = webClientBuilder
            .baseUrl(pongServiceUrl)
            .build();
        log.info("Initialized WebClient with pong service URL: {}", pongServiceUrl);
        this.messageService = messageService;
    }
    
    @Scheduled(fixedRate = 1000)
    public void schedulePeriodicPing() {
        sendPing().subscribe();
    }
    
    public Mono<PingResult> sendPing() {
        auditLogger.info("Try to send ping request to Pong service");
        String requestId = UUID.randomUUID().toString();
        
        if (!globalRateLimiter.tryAcquire()) {
            PingRecord record = new PingRecord();
            record.setMessage(PingStatus.RATE_LIMITED_LOCAL.getDescription());
            record.setStatus(429);
            record.setTimestamp(LocalDateTime.now());
            record.setRequestId(requestId);
            messageService.sendPingMessage(record);
            auditLogger.info("Result: {}", record);
            
            return Mono.just(new PingResult(PingStatus.RATE_LIMITED_LOCAL, 
                PingStatus.RATE_LIMITED_LOCAL.getDescription()));
        }

        return webClient.get()
            .uri("/api/pong")
            .retrieve()
            .onStatus(status -> status.is4xxClientError(), response -> 
                Mono.just(new RuntimeException(PingStatus.RATE_LIMITED_REMOTE.getDescription())))
            .toEntity(String.class)
            .map(response -> {
                PingRecord record = new PingRecord();
                record.setMessage(response.getBody());
                record.setStatus(response.getStatusCode().value());
                record.setTimestamp(LocalDateTime.now());
                record.setRequestId(requestId);
                messageService.sendPingMessage(record);
                auditLogger.info("Result: {}", record);
                
                return new PingResult(PingStatus.SUCCESS, response.getBody());
            })
            .onErrorResume(e -> {
                PingRecord record = new PingRecord();
                record.setMessage(e.getMessage());
                record.setStatus(429);
                record.setTimestamp(LocalDateTime.now());
                record.setRequestId(requestId);
                messageService.sendPingMessage(record);
                auditLogger.info("Result: {}", record);
                
                return Mono.just(new PingResult(PingStatus.RATE_LIMITED_REMOTE, e.getMessage()));
            });
    }
} 