package com.challenge.ping.service;

import com.challenge.ping.model.PingRecord;
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
public class PingMessageService {
    private final RocketMQTemplate rocketMQTemplate;
    private static final String TOPIC = "PING_TOPIC";
    
    public void sendPingMessage(PingRecord record) {
        try {
            Message<PingRecord> message = MessageBuilder.withPayload(record)
                .setHeader(RocketMQHeaders.KEYS, record.getRequestId())
                .build();
                
            rocketMQTemplate.send(TOPIC, message);
            log.info("消息发送成功 - RequestId: {}", record.getRequestId());
        } catch (Exception e) {
            log.error("消息发送失败: {}", e.getMessage(), e);
            throw new RuntimeException("消息发送失败", e);
        }
    }
    
} 