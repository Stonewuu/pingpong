package com.challenge.pong.consumer

import com.challenge.pong.model.PongRecord
import com.challenge.pong.service.PongMessageService
import spock.lang.Specification
import java.time.LocalDateTime

class MQConsumerServiceSpec extends Specification {
    
    MQConsumerService mqConsumerService
    PongMessageService messageService
    MQConsumerService.PongResponseConsumers consumer
    
    def setup() {
        messageService = Mock(PongMessageService)
        mqConsumerService = new MQConsumerService(messageService: messageService)
        consumer = new MQConsumerService.PongResponseConsumers(mqConsumerService)
    }
    
    def "should handle message successfully"() {
        given:
        def timestamp = LocalDateTime.now()
        def record = new PongRecord(
            message: "Test message",
            status: 200,
            timestamp: timestamp,
            requestId: "test-id"
        )
        
        when:
        consumer.onMessage(record)
        
        then:
        1 * messageService.consumePongResponse(record) >> true
        noExceptionThrown()
    }
    
    def "should handle null message gracefully"() {
        when:
        consumer.onMessage(null)
        
        then:
        1 * messageService.consumePongResponse(null) >> false
        noExceptionThrown()
    }
    
    def "should handle message processing exception"() {
        given:
        def record = new PongRecord(
            message: "Test message",
            status: 200,
            timestamp: LocalDateTime.now(),
            requestId: "test-id"
        )
        def expectedException = new RuntimeException("handle message failed")
        
        when:
        consumer.onMessage(record)
        
        then:
        1 * messageService.consumePongResponse(record) >> { throw expectedException }
        noExceptionThrown()
    }
    
    def "should handle message with missing fields"() {
        given:
        def record = new PongRecord()
        
        when:
        consumer.onMessage(record)
        
        then:
        1 * messageService.consumePongResponse(record) >> true
        noExceptionThrown()
    }
} 