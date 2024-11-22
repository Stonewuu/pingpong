package com.challenge.pong.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQUtil;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

@Configuration
public class RocketMQConfig {
    
    @Value("${rocketmq.name-server}")
    private String nameServer;
    
    @Value("${rocketmq.producer.group}")
    private String producerGroup;
    
    @Bean(destroyMethod = "destroy")
    public RocketMQTemplate rocketMQTemplate() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr(nameServer);
        producer.setProducerGroup(producerGroup);
        
        // 设置生产者参数
        producer.setRetryTimesWhenSendFailed(3);
        producer.setRetryTimesWhenSendAsyncFailed(3);
        producer.setSendMsgTimeout(5000);
        producer.setVipChannelEnabled(false);  // 禁用VIP通道
        producer.setInstanceName(producerGroup + "@" + System.currentTimeMillis());  // 设置唯一实例名
        
        // 启动生产者
        int retryCount = 0;
        Exception lastException = null;
        while (retryCount < 5) {
            try {
                producer.start();
                break;
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                if (retryCount == 5) {
                    throw new RuntimeException("Failed to start RocketMQ producer after 5 retries", lastException);
                }
                Thread.sleep(5000);
            }
        }
        
        // 创建并配置RocketMQTemplate
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setProducer(producer);
        rocketMQTemplate.setMessageConverter(new MappingJackson2MessageConverter());
        
        return rocketMQTemplate;
    }
    
    private void destroy() {
        try {
            if (rocketMQTemplate() != null) {
                rocketMQTemplate().destroy();
            }
        } catch (Exception e) {
        }
    }
} 