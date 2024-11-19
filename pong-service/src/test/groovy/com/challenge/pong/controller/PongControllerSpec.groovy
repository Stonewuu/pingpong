package com.challenge.pong.controller

import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject

class PongControllerSpec extends Specification {

    @Subject
    PongController controller
    
    def setup() {
        controller = new PongController()
    }
    
    def "should handle successful ping request"() {
        when:
        def result = controller.handlePing().block()
        
        then:
        result instanceof ResponseEntity
        result.statusCodeValue == 200
        result.body == "World"
        println "result: $result"
    }
    
    def "should handle rate limiting"() {
        given:
        controller.handlePing().block() // 消耗第一个配额
        
        when:
        def result = controller.handlePing().block()
        
        then:
        result instanceof ResponseEntity
        result.statusCodeValue == 429
        result.body == "Rate limited by Pong service"
    }
    
    def "should verify rate limiter permits recovery"() {
        given:
        controller.handlePing().block() // 消耗配额
        
        when:
        Thread.sleep(1100) // 等待超过1秒让令牌桶恢复
        def result = controller.handlePing().block()
        
        then:
        result instanceof ResponseEntity
        result.statusCodeValue == 200
        result.body == "World"
    }
    
    def "should handle concurrent requests correctly"() {
        when:
        def results = (1..3).collect { 
            controller.handlePing() 
        }.collect { mono ->
            mono.block()
        }
        
        then:
        results.count { it.statusCodeValue == 200 } == 1
        results.count { it.statusCodeValue == 429 } == 2
    }
} 