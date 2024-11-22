package com.challenge.pong.service;

import com.challenge.pong.model.PongRecord;
import com.challenge.pong.repository.PongRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PongMessageService {
    private final RocketMQTemplate rocketMQTemplate;
    private final PongRecordRepository repository;
    private static final String TOPIC = "PONG_RESPONSE_TOPIC";
    
    public void sendPongResponse(PongRecord record) {
        Message<PongRecord> message = MessageBuilder.withPayload(record)
            .setHeader(RocketMQHeaders.KEYS, record.getRequestId())
            .build();
            
        rocketMQTemplate.syncSend(TOPIC, message);
        log.info("Sent message to MQ: {}", record);
    }
    
    public void consumePongResponse(PongRecord record) {
        repository.save(record);
        log.info("Saved record to database: {}", record);
    }
} 