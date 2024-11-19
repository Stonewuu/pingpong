package com.challenge.ping.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    
    public PingService(
            WebClient.Builder webClientBuilder,
            GlobalRateLimiter globalRateLimiter) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
        this.globalRateLimiter = globalRateLimiter;
        auditLogger.info("Ping Service Started");
    }
    
    @Scheduled(fixedRate = 1000)
    public void schedulePeriodicPing() {
        sendPing().subscribe();
    }
    
    public Mono<PingResult> sendPing() {
        log.debug("Try to send ping request to Pong service");
        if (!globalRateLimiter.tryAcquire()) {
            var result = new PingResult(PingStatus.RATE_LIMITED_LOCAL, 
                "Rate limited by global limiter");
            auditLogger.info("Result: {}", result);
            return Mono.just(result);
        }
        
        auditLogger.info("Attempting to send ping request");
        return webClient.get()
            .uri("/api/pong")
            .attribute("parameter", "hello")
            .retrieve()
            .onStatus(
                HttpStatus.TOO_MANY_REQUESTS::equals,
                response -> response.bodyToMono(String.class)
                    .map(body -> {
                        var result = new PingResult(PingStatus.RATE_LIMITED_REMOTE, body);
                        auditLogger.info("Result: {}", result);
                        return result;
                    })
                    .then(Mono.empty())
            )
            .toEntity(String.class)
            .map(response -> {
                if (response.getStatusCode() == HttpStatus.OK) {
                    var result = new PingResult(PingStatus.SUCCESS, response.getBody());
                    auditLogger.info("Result: {}", result);
                    return result;
                }
                return new PingResult(PingStatus.SUCCESS, "Unexpected response");
            })
            .doOnError(error -> {
                if (!(error instanceof WebClientResponseException.TooManyRequests)) {
                    log.error("Error during ping request - Error: {}", error);
                    auditLogger.error("Ping request failed - Error: {}", error.getMessage());
                }
            })
            .onErrorResume(e -> Mono.just(
                new PingResult(PingStatus.SUCCESS, "Error: " + e.getMessage())
            ));
    }
} 