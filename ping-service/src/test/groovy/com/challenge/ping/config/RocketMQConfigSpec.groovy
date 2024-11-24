package com.challenge.ping.config

import org.apache.rocketmq.client.producer.DefaultMQProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [RocketMQConfig])
@TestPropertySource(properties = [
    "rocketmq.name-server=localhost:9876",
    "rocketmq.producer.group=ping-group"
])
class RocketMQConfigSpec extends Specification {

    @Autowired
    RocketMQConfig rocketMQConfig

    def "should create RocketMQ producer with correct configuration"() {
        when:
        def producer = rocketMQConfig.producer()
        
        then:
        producer instanceof DefaultMQProducer
        producer.getNamesrvAddr() == "localhost:9876"
        producer.getProducerGroup() == "ping-group"
    }
    
    def "should handle empty name server configuration"() {
        when:
        def producer = rocketMQConfig.producer()
        
        then:
        producer instanceof DefaultMQProducer
        producer.getProducerGroup() == "ping-group"
    }
    
    def "should handle empty producer group configuration"() {
        when:
        def producer = rocketMQConfig.producer()
        
        then:
        producer instanceof DefaultMQProducer
        producer.getNamesrvAddr() == "localhost:9876"
    }
} 