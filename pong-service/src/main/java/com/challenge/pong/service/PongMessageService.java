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

    
    public boolean consumePongResponse(PongRecord record) {
        if (record == null) {
            return false;
        }
        try {
            repository.save(record);
            log.info("message saved to database: {}", record);
        } catch (Exception e) {
            log.error("handle message failed: {}", e.getMessage(), e);
            throw new RuntimeException("handle message failed", e);
        }
        return true;
    }
} 