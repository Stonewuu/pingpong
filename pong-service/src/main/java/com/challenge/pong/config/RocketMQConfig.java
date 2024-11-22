package com.challenge.pong.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RocketMQConfig {
    
    @Value("${rocketmq.name-server}")
    private String nameServer;
    
    @Value("${rocketmq.producer.group}")
    private String producerGroup;
    
    
    @Bean
    public RocketMQTemplate rocketMQTemplate() {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr(nameServer);
        producer.setProducerGroup(producerGroup);
        
        // 设置生产者参数
        producer.setDefaultTopicQueueNums(4);
        producer.setRetryTimesWhenSendFailed(3);
        producer.setRetryTimesWhenSendAsyncFailed(3);
        producer.setSendMsgTimeout(10000);
        producer.setVipChannelEnabled(false);
        producer.setInstanceName(producerGroup + "@" + System.currentTimeMillis());
        producer.setCompressMsgBodyOverHowmuch(4096);
        producer.setMaxMessageSize(4194304);
        producer.setRetryAnotherBrokerWhenNotStoreOK(true);
        
        // 创建并配置消息转换器
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converter.setObjectMapper(objectMapper);
        
        // 创建并配置RocketMQTemplate
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setProducer(producer);
        rocketMQTemplate.setMessageConverter(converter);
        
        return rocketMQTemplate;
    }
} 