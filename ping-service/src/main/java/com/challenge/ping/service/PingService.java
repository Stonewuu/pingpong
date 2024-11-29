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

/**
 * Ping 服务类，负责向 Pong 服务发送定期的 ping 请求
 * 并处理响应结果，包含速率限制和消息记录功能
 */
@Service
@Slf4j
public class PingService {
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Ping 状态枚举，定义了所有可能的 ping 结果状态
     */
    public enum PingStatus {
        SUCCESS("Pong responded successfully"),           // Pong 服务成功响应
        RATE_LIMITED_REMOTE("Rate limited by Pong service"), // 被 Pong 服务限流
        RATE_LIMITED_LOCAL("Rate limited by Ping service");  // 被 Ping 服务限流
        
        private final String description;
        
        PingStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Ping 结果记录类，存储每次 ping 请求的结果信息
     */
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
    
    // WebClient 用于发送 HTTP 请求
    private final WebClient webClient;
    // 全局速率限制器
    private final GlobalRateLimiter globalRateLimiter;
    // 消息服务，用于发送 ping 记录
    private final PingMessageService messageService;
    
    @Value("${ping.pong-service.url}")
    private String pongServiceUrl;
    
    /**
     * 构造函数，初始化 WebClient 和其他必要组件
     */
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
    
    /**
     * 定时任务，每秒执行一次 ping 请求
     */
    @Scheduled(fixedRate = 1000)
    public void schedulePeriodicPing() {
        sendPing().subscribe();
    }
    
    /**
     * 发送 ping 请求的主要方法
     * 包含以下功能：
     * 1. 本地速率限制检查
     * 2. 发送 HTTP 请求到 Pong 服务
     * 3. 处理响应结果
     * 4. 错误处理
     * 5. 记录审计日志
     * 
     * @return Mono<PingResult> 包含 ping 请求的结果
     */
    public Mono<PingResult> sendPing() {
        auditLogger.info("Try to send ping request to Pong service");
        String requestId = UUID.randomUUID().toString();
        
        // 检查速率限制
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

        // 发送请求并处理响应
        return webClient.get()
            .uri("/api/pong")
            .retrieve()
            // 处理 4xx 错误响应
            .onStatus(status -> status.is4xxClientError(), response -> 
                Mono.just(new RuntimeException(PingStatus.RATE_LIMITED_REMOTE.getDescription())))
            .toEntity(String.class)
            // 处理成功响应
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
            // 处理错误情况
            .onErrorResume(e -> {
                PingRecord record = new PingRecord();
                record.setMessage(PingStatus.RATE_LIMITED_REMOTE.getDescription());
                record.setStatus(429);
                record.setTimestamp(LocalDateTime.now());
                record.setRequestId(requestId);
                messageService.sendPingMessage(record);
                auditLogger.info("Result: {}", record);
                
                return Mono.just(new PingResult(PingStatus.RATE_LIMITED_REMOTE, 
                    PingStatus.RATE_LIMITED_REMOTE.getDescription()));
            });
    }
} 