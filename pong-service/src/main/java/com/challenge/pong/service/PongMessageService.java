package com.challenge.pong.service;

import com.challenge.pong.model.PongRecord;
import com.challenge.pong.repository.PongRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PongMessageService {
    private final PongRecordRepository repository;

    
    public void consumePongResponse(PongRecord record) {
        try {
            log.info("接收到消息: {}", record);
            repository.save(record);
            log.info("消息成功保存到数据库: {}", record);
        } catch (Exception e) {
            log.error("消息处理失败: {}", e.getMessage(), e);
            throw new RuntimeException("消息处理失败", e);
        }
    }
} 