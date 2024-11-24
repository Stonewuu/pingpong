package com.challenge.pong.model

import spock.lang.Specification
import java.time.LocalDateTime

class PongRecordSpec extends Specification {
    
    def "should create PongRecord with default constructor"() {
        when:
        def record = new PongRecord()
        
        then:
        record.message == null
        record.status == 0
        record.timestamp == null
        record.requestId == null
    }
    
    def "should set and get message"() {
        given:
        def record = new PongRecord()
        def message = "Test message"
        
        when:
        record.setMessage(message)
        
        then:
        record.getMessage() == message
    }
    
    def "should set and get status"() {
        given:
        def record = new PongRecord()
        def status = 200
        
        when:
        record.setStatus(status)
        
        then:
        record.getStatus() == status
    }
    
    def "should set and get timestamp"() {
        given:
        def record = new PongRecord()
        def timestamp = LocalDateTime.now()
        
        when:
        record.setTimestamp(timestamp)
        
        then:
        record.getTimestamp() == timestamp
    }
    
    def "should set and get requestId"() {
        given:
        def record = new PongRecord()
        def requestId = "test-request-id"
        
        when:
        record.setRequestId(requestId)
        
        then:
        record.getRequestId() == requestId
    }
    
    
    def "should implement toString"() {
        given:
        def timestamp = LocalDateTime.now()
        def record = new PongRecord(
            message: "Test message",
            status: 200,
            timestamp: timestamp,
            requestId: "test-id"
        )
        
        when:
        def result = record.toString()
        
        then:
        result.contains("Test message")
        result.contains("200")
        result.contains("test-id")
        result.contains(timestamp.toString())
    }
    
    
} 