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
    // 响应数据结构：包含消息内容、状态码和请求ID
    private record PongResponse(String message, int status, String requestId) {}
    
    // 用于记录上一次请求的时间戳
    private volatile long lastRequestTime = 0;
    // 定义请求间隔时间为1秒
    private static final long INTERVAL_MS = 1000; 
    // 用于记录限流相关的日志
    private static final Logger RATE_LIMIT_LOG = LoggerFactory.getLogger("RATE_LIMIT");
    // 用于记录审计相关的日志
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");
    
    @Autowired
    private PongMessageService messageService;
    
    /**
     * 处理ping请求的端点
     * 实现了基于时间间隔的简单限流机制
     * @return 响应实体
     */
    @GetMapping("/pong")
    public Mono<ResponseEntity<String>> handlePing() {
        return Mono.fromSupplier(() -> {
            // 记录请求处理开始
            log.info("Processing ping request");
            // 获取当前时间戳
            long currentTime = System.currentTimeMillis();
            // 标记是否通过限流检查
            boolean acquired = false;
            // 生成唯一请求ID
            String requestId = UUID.randomUUID().toString();
            
            // 使用同步块检查和更新限流状态
            synchronized (this) {
                if (currentTime - lastRequestTime >= INTERVAL_MS) {
                    acquired = true;
                    lastRequestTime = currentTime;
                }
            }
            
            // 根据限流检查结果构造响应
            var response = acquired 
                ? new PongResponse("World", 200, requestId)
                : new PongResponse("Rate limited by Pong service", 429, requestId);
            
            // 记录请求处理结果
            if (acquired) {
                AUDIT_LOG.info("Request processed successfully");
            } else {
                RATE_LIMIT_LOG.info("Request rate limited");
            }
            
            // 根据响应状态返回不同的HTTP响应
            if (response.status() == 200) {
                return ResponseEntity.ok(response.message());
            } else if (response.status() == 429) {
                return ResponseEntity.status(429).body(response.message());
            } else {
                return ResponseEntity.internalServerError().body("Unknown error");
            }
        });
    }
} 