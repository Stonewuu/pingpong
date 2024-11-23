package com.challenge.pong.consumer;

import com.challenge.pong.model.PongRecord;
import com.challenge.pong.service.PongMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


@Slf4j
@Component
public class MQConsumerService {
    @Autowired
    private PongMessageService messageService;

    @Service
    @RocketMQMessageListener(topic = "PING_TOPIC", consumerGroup = "pong-consumer-group")
    public class PongResponseConsumers implements RocketMQListener<PongRecord> {
        @Override
        public void onMessage(PongRecord message) {
            log.info("received message: {}", message);
            try {
                messageService.consumePongResponse(message);
                log.info("message processed: {}", message.getRequestId());
            } catch (Exception e) {
                log.error("message processing failed: {}", e.getMessage(), e);
            }
        }
    }
}