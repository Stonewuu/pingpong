package com.challenge.pong.service

import com.challenge.pong.model.PongRecord
import com.challenge.pong.repository.PongRecordRepository
import spock.lang.Specification
import java.time.LocalDateTime

class PongMessageServiceSpec extends Specification {
    
    PongMessageService messageService
    PongRecordRepository repository
    
    def setup() {
        repository = Mock(PongRecordRepository)
        messageService = new PongMessageService(repository)
    }
    
    def "should successfully consume and save pong response"() {
        given:
        def timestamp = LocalDateTime.now()
        def record = new PongRecord(
            message: "Test message",
            status: 200,
            timestamp: timestamp,
            requestId: "test-id"
        )
        
        when:
        def result = messageService.consumePongResponse(record)
        
        then:
        1 * repository.save(record) >> record
        result == true
        noExceptionThrown()
    }
    
    def "should handle null record gracefully"() {
        when:
        def result = messageService.consumePongResponse(null)
        
        then:
        result == false
        0 * repository.save(_)
    }
    
    def "should handle record with missing fields"() {
        given:
        def record = new PongRecord()
        
        when:
        def result = messageService.consumePongResponse(record)
        
        then:
        1 * repository.save(record) >> record
        result == true
        noExceptionThrown()
    }
    
    def "should verify saved record matches input"() {
        given:
        def timestamp = LocalDateTime.now()
        def record = new PongRecord(
            message: "Test message",
            status: 200,
            timestamp: timestamp,
            requestId: "test-id"
        )
        
        when:
        def result = messageService.consumePongResponse(record)
        
        then:
        1 * repository.save({ savedRecord ->
            verifyAll(savedRecord) {
                it.message == record.message
                it.status == record.status
                it.timestamp == record.timestamp
                it.requestId == record.requestId
            }
            true
        }) >> record
        result == true
        noExceptionThrown()
    }
    
    def "should handle database exception correctly"() {
        given:
        def timestamp = LocalDateTime.now()
        def record = new PongRecord(
            message: "Test message",
            status: 200,
            timestamp: timestamp,
            requestId: "test-id"
        )
        def expectedException = new RuntimeException("database connection failed")
        
        when:
        repository.save(record) >> { throw expectedException }
        messageService.consumePongResponse(record)
        
        then:
        def thrown = thrown(RuntimeException)
        thrown.message == "handle message failed"
        thrown.cause == expectedException
        1 * repository.save(record) >> { throw expectedException }
    }
    
} 