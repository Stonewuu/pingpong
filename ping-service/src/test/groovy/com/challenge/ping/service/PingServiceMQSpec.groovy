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
import org.apache.rocketmq.spring.core.RocketMQTemplate

class PingServiceMQSpec extends Specification {
    
    PingService pingService
    WebClient webClient
    GlobalRateLimiter globalRateLimiter
    PingMessageService pingMessageService
    RocketMQTemplate rocketMQTemplate
    
    def setup() {
        def webClientBuilder = Mock(WebClient.Builder)
        webClient = Mock(WebClient)
        globalRateLimiter = Mock(GlobalRateLimiter)
        rocketMQTemplate = Mock(RocketMQTemplate)
        pingMessageService = new PingMessageService(rocketMQTemplate)

        webClientBuilder.baseUrl(_) >> webClientBuilder
        webClientBuilder.build() >> webClient
        
        pingService = new PingService(webClientBuilder, globalRateLimiter, "http://localhost:8081", pingMessageService)
    }

    def "should send message to MQ for successful ping"() {
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
        1 * rocketMQTemplate.send("PING_TOPIC", { message ->
            assert message.payload.status == 200
            assert message.payload.message == "World"
            assert message.headers.get("KEYS") != null
            true
        })
        result.status == PingService.PingStatus.SUCCESS
        result.response == "World"
    }

    def "should send message to MQ for rate limited request"() {
        given:
        globalRateLimiter.tryAcquire() >> false

        when:
        def result = pingService.sendPing().block()

        then:
        1 * rocketMQTemplate.send("PING_TOPIC", { message ->
            assert message.payload.status == 429
            assert message.payload.message == PingService.PingStatus.RATE_LIMITED_LOCAL.getDescription()
            assert message.headers.get("KEYS") != null
            true
        })
        result.status == PingService.PingStatus.RATE_LIMITED_LOCAL
        result.response == PingService.PingStatus.RATE_LIMITED_LOCAL.getDescription()
    }

    def "should handle message sending failure for successful ping"() {
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
        
        rocketMQTemplate.send(_, _) >> { throw new RuntimeException("Failed to send message") }

        when:
        pingService.sendPing().block()

        then:
        thrown(RuntimeException)
    }

    def "should handle message sending failure for rate limited request"() {
        given:
        globalRateLimiter.tryAcquire() >> false
        
        // 模拟 RocketMQ 发送消息失败
        rocketMQTemplate.send(_, _) >> { throw new RuntimeException("Failed to send message") }

        when:
        pingService.sendPing().block()

        then:
        thrown(RuntimeException)
    }

    def "should handle message sending failure for remote rate limited request"() {
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
        
        // 模拟 RocketMQ 发送消息失败
        rocketMQTemplate.send(_, _) >> { throw new RuntimeException("Failed to send message") }

        when:
        pingService.sendPing().block()

        then:
        thrown(RuntimeException)
    }
} 