package com.challenge.pong.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
public class PongController {
    private record PongResponse(String message, int status) {}
    
    private final RateLimiter rateLimiter;
    private static final Logger RATE_LIMIT_LOG = LoggerFactory.getLogger("RATE_LIMIT");
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");
    
    public PongController() {
        this.rateLimiter = RateLimiter.create(1.0); // 每秒1个请求
    }
    
    @GetMapping("/pong")
    public Mono<ResponseEntity<String>> handlePing() {
        return Mono.fromSupplier(() -> {
            boolean acquired = rateLimiter.tryAcquire();
            var response = acquired 
                ? new PongResponse("World", 200)
                : new PongResponse("Rate limited by Pong service", 429);
                
            if (acquired) {
                log.info("Processing ping request");
                AUDIT_LOG.info("Request processed successfully");
            } else {
                RATE_LIMIT_LOG.info("Request rate limited");
                log.info("Rate limiting ping request");
            }
            
            return switch (response.status()) {
                case 200 -> ResponseEntity.ok(response.message());
                case 429 -> ResponseEntity.status(429).body(response.message());
                default -> ResponseEntity.internalServerError().body("Unknown error");
            };
        });
    }
} 