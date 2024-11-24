package com.challenge.ping.service

import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration
import reactor.core.publisher.Mono
import com.challenge.ping.PingApplication
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import com.challenge.ping.model.PingRecord

class PingServiceSpec extends Specification {
    
    PingService pingService
    WebClient webClient
    GlobalRateLimiter globalRateLimiter
    PingMessageService pingMessageService
    
    def setup() {
        def webClientBuilder = Mock(WebClient.Builder)
        webClient = Mock(WebClient)
        globalRateLimiter = Mock(GlobalRateLimiter)
        pingMessageService = Mock(PingMessageService)

        webClientBuilder.baseUrl(_) >> webClientBuilder
        webClientBuilder.build() >> webClient
        
        pingService = new PingService(webClientBuilder, globalRateLimiter, "http://localhost:8081", pingMessageService)

        // 清理可能存在的文件锁
        new File("ping-rate.lock").delete()
        new File("ping-rate.data").delete()
        
        Thread.sleep(1000)
    }

    def "should handle successful ping"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        def response = Mock(ResponseEntity)
        globalRateLimiter.tryAcquire() >> true
        
        response.getStatusCode() >> HttpStatus.OK
        response.getBody() >> "World"
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.just(response)

        when:
        def result = pingService.sendPing().block()

        then:
        result.status == PingService.PingStatus.SUCCESS
        result.response == "World"
    }
    
    def "should handle local rate limiting"() {
        given:
        globalRateLimiter.tryAcquire() >> false

        when:
        def result = pingService.sendPing().block()

        then:
        result.status == PingService.PingStatus.RATE_LIMITED_LOCAL
        result.response == PingService.PingStatus.RATE_LIMITED_LOCAL.getDescription()
    }
    
    def "should handle remote rate limiting"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        globalRateLimiter.tryAcquire() >> true
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.error(
            WebClientResponseException.TooManyRequests.create(
                429,
                "Rate limited by Pong service",
                null,
                "Rate limited".getBytes(),
                null,
                null
            )
        )

        when:
        def result = pingService.sendPing().block()
        
        then:
        result.status == PingService.PingStatus.RATE_LIMITED_REMOTE
        result.response == PingService.PingStatus.RATE_LIMITED_REMOTE.getDescription()
    }
    
    def "should handle other remote errors"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        globalRateLimiter.tryAcquire() >> true
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.error(
            new RuntimeException("Connection timeout")
        )

        when:
        def result = pingService.sendPing().block()
        
        then:
        result.status == PingService.PingStatus.RATE_LIMITED_REMOTE
        result.response == PingService.PingStatus.RATE_LIMITED_REMOTE.getDescription()
    }
    
    def "should handle periodic ping scheduling"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        def response = Mock(ResponseEntity)
        globalRateLimiter.tryAcquire() >> true
        
        response.getStatusCode() >> HttpStatus.OK
        response.getBody() >> "World"
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.just(response)

        when:
        pingService.schedulePeriodicPing()
        Thread.sleep(100) // 给一点时间让异步操作完成

        then:
        1 * pingMessageService.sendPingMessage(_)
    }

    def "should handle null response body"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        def response = Mock(ResponseEntity)
        globalRateLimiter.tryAcquire() >> true
        
        response.getStatusCode() >> HttpStatus.OK
        response.getBody() >> null  // 测试空响应体
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.just(response)

        when:
        def result = pingService.sendPing().block()

        then:
        result.status == PingService.PingStatus.SUCCESS
        result.response == null
    }

    def "should handle different HTTP status codes"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        def response = Mock(ResponseEntity)
        globalRateLimiter.tryAcquire() >> true
        
        response.getStatusCode() >> statusCode
        response.getBody() >> "Response"
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.just(response)

        when:
        def result = pingService.sendPing().block()

        then:
        result.status == expectedStatus
        result.response == expectedResponse

        where:
        statusCode           | expectedStatus                        | expectedResponse
        HttpStatus.OK       | PingService.PingStatus.SUCCESS        | "Response"
        HttpStatus.CREATED  | PingService.PingStatus.SUCCESS        | "Response"
        HttpStatus.ACCEPTED | PingService.PingStatus.SUCCESS        | "Response"
    }

    def "should handle various error scenarios"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        globalRateLimiter.tryAcquire() >> true
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.error(error)

        when:
        def result = pingService.sendPing().block()
        
        then:
        result.status == expectedStatus
        result.response == expectedResponse

        where:
        error                                                           | expectedStatus                              | expectedResponse
        new RuntimeException("Unknown error")                           | PingService.PingStatus.RATE_LIMITED_REMOTE | PingService.PingStatus.RATE_LIMITED_REMOTE.getDescription()
        WebClientResponseException.BadRequest.create(400, "", null, null, null, null) | PingService.PingStatus.RATE_LIMITED_REMOTE | PingService.PingStatus.RATE_LIMITED_REMOTE.getDescription()
        WebClientResponseException.ServiceUnavailable.create(503, "", null, null, null, null) | PingService.PingStatus.RATE_LIMITED_REMOTE | PingService.PingStatus.RATE_LIMITED_REMOTE.getDescription()
    }

    def "should handle null request ID generation"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        def response = Mock(ResponseEntity)
        globalRateLimiter.tryAcquire() >> true
        
        response.getStatusCode() >> HttpStatus.OK
        response.getBody() >> "World"
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.just(response)

        when:
        def result = pingService.sendPing().block()

        then:
        1 * pingMessageService.sendPingMessage({ record ->
            assert record.requestId != null
            true
        })
        result.status == PingService.PingStatus.SUCCESS
    }

    def "should verify PingRecord equals method"() {
        given:
        def record1 = new PingRecord(
            id: 1L,
            message: "test",
            status: 200,
            timestamp: LocalDateTime.now(),
            requestId: "123"
        )
        def record2 = new PingRecord(
            id: 1L,
            message: "test",
            status: 200,
            timestamp: record1.timestamp,
            requestId: "123"
        )
        def record3 = new PingRecord(
            id: 2L,
            message: "test",
            status: 200,
            timestamp: record1.timestamp,
            requestId: "123"
        )
        def notPingRecord = new Object()

        expect:
        record1 == record2
        record1 != record3
        record1 != null
        record1 != notPingRecord
        record1.hashCode() == record2.hashCode()
        record1.hashCode() != record3.hashCode()
    }

    def "should verify PingRecord equals with different fields"() {
        given:
        def timestamp = LocalDateTime.now()
        def baseRecord = new PingRecord(
            id: 1L,
            message: "test",
            status: 200,
            timestamp: timestamp,
            requestId: "123"
        )
        def differentMessage = new PingRecord(
            id: 1L,
            message: "different",
            status: 200,
            timestamp: timestamp,
            requestId: "123"
        )
        def differentStatus = new PingRecord(
            id: 1L,
            message: "test",
            status: 400,
            timestamp: timestamp,
            requestId: "123"
        )
        def differentTimestamp = new PingRecord(
            id: 1L,
            message: "test",
            status: 200,
            timestamp: timestamp.plusDays(1),
            requestId: "123"
        )
        def differentRequestId = new PingRecord(
            id: 1L,
            message: "test",
            status: 200,
            timestamp: timestamp,
            requestId: "456"
        )

        expect:
        baseRecord != differentMessage
        baseRecord != differentStatus
        baseRecord != differentTimestamp
        baseRecord != differentRequestId
    }
} 