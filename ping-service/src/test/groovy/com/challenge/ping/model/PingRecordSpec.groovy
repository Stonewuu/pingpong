package com.challenge.ping.model

import spock.lang.Specification
import java.time.LocalDateTime

class PingRecordSpec extends Specification {
    
    def "should create PingRecord with default constructor"() {
        when:
        def record = new PingRecord()
        
        then:
        record.message == null
        record.status == 0
        record.timestamp == null
        record.requestId == null
    }
    
    def "should set and get message"() {
        given:
        def record = new PingRecord()
        def message = "Test message"
        
        when:
        record.setMessage(message)
        
        then:
        record.getMessage() == message
    }
    
    def "should set and get status"() {
        given:
        def record = new PingRecord()
        def status = 200
        
        when:
        record.setStatus(status)
        
        then:
        record.getStatus() == status
    }
    
    def "should set and get timestamp"() {
        given:
        def record = new PingRecord()
        def timestamp = LocalDateTime.now()
        
        when:
        record.setTimestamp(timestamp)
        
        then:
        record.getTimestamp() == timestamp
    }
    
    def "should set and get requestId"() {
        given:
        def record = new PingRecord()
        def requestId = "test-request-id"
        
        when:
        record.setRequestId(requestId)
        
        then:
        record.getRequestId() == requestId
    }
    
    def "should implement equals and hashCode"() {
        given:
        def timestamp = LocalDateTime.now()
        def record1 = new PingRecord(
            message: "Test message",
            status: 200,
            timestamp: timestamp,
            requestId: "test-id"
        )
        def record2 = new PingRecord(
            message: "Test message",
            status: 200,
            timestamp: timestamp,
            requestId: "test-id"
        )
        def record3 = new PingRecord(
            message: "Different message",
            status: 404,
            timestamp: timestamp,
            requestId: "different-id"
        )
        
        expect:
        record1 == record2
        record1 != record3
        record1.hashCode() == record2.hashCode()
        record1.hashCode() != record3.hashCode()
    }
    
    def "should implement toString"() {
        given:
        def timestamp = LocalDateTime.now()
        def record = new PingRecord(
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
    
    def "should handle null values in equals and hashCode"() {
        given:
        def record1 = new PingRecord()
        def record2 = new PingRecord()
        
        expect:
        record1 == record2
        record1.hashCode() == record2.hashCode()
    }
    
    
    def "should compare with different object types"() {
        given:
        def record = new PingRecord()
        def otherObject = "not a record"
        
        expect:
        record != otherObject
    }
    
} 