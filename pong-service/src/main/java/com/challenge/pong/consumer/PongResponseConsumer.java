package com.challenge.pong.consumer;

import com.challenge.pong.model.PongRecord;
import com.challenge.pong.service.PongMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "PONG_RESPONSE_TOPIC",
    consumerGroup = "pong-consumer-group"
)
public class PongResponseConsumer implements RocketMQListener<PongRecord> {
    @Autowired
    private PongMessageService messageService;
    
    @Override
    public void onMessage(PongRecord message) {
        messageService.consumePongResponse(message);
    }
} 