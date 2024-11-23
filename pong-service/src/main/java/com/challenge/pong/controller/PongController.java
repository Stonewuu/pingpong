package com.challenge.pong.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import com.challenge.pong.service.PongMessageService;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Slf4j
public class PongController {
    private record PongResponse(String message, int status, String requestId) {}
    
    private volatile long lastRequestTime = 0;
    private static final long INTERVAL_MS = 1000; // 1秒的间隔
    private static final Logger RATE_LIMIT_LOG = LoggerFactory.getLogger("RATE_LIMIT");
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");
    
    @Autowired
    private PongMessageService messageService;
    
    @GetMapping("/pong")
    public Mono<ResponseEntity<String>> handlePing() {
        return Mono.fromSupplier(() -> {
            long currentTime = System.currentTimeMillis();
            boolean acquired = false;
            String requestId = UUID.randomUUID().toString();
            
            synchronized (this) {
                if (currentTime - lastRequestTime >= INTERVAL_MS) {
                    acquired = true;
                    lastRequestTime = currentTime;
                }
            }
            
            var response = acquired 
                ? new PongResponse("World", 200, requestId)
                : new PongResponse("Rate limited by Pong service", 429, requestId);
            
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