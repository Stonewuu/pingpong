import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration
import reactor.core.publisher.Mono
import com.challenge.ping.service.GlobalRateLimiter
import com.challenge.ping.service.PingService
import com.challenge.ping.PingApplication
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus

@SpringBootTest
@ContextConfiguration(classes = [PingApplication.class])
class PingServiceSpec extends Specification {
    
    PingService pingService
    WebClient webClient
    GlobalRateLimiter globalRateLimiter
    
    def setup() {
        def webClientBuilder = Mock(WebClient.Builder)
        webClient = Mock(WebClient)
        globalRateLimiter = Mock(GlobalRateLimiter)
        
        webClientBuilder.baseUrl(_) >> webClientBuilder
        webClientBuilder.build() >> webClient
        
        pingService = new PingService(webClientBuilder, globalRateLimiter)

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
        requestHeadersSpec.attribute("parameter", "hello") >> requestHeadersSpec
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
        result.response.contains("Rate limited by global limiter")
    }
    
    def "should handle remote rate limiting"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        globalRateLimiter.tryAcquire() >> true
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.attribute("parameter", "hello") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.error(
            WebClientResponseException.TooManyRequests.create(
                429,
                "Too Many Requests",
                null,
                "Rate limited".getBytes(),
                null,
                null
            )
        )

        when:
        def result = pingService.sendPing().block()
        
        then:
        result.status == PingService.PingStatus.SUCCESS
        result.response.contains("Too Many Requests")
    }

    def "should handle error response with status #statusCode"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        globalRateLimiter.tryAcquire() >> true
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.attribute("parameter", "hello") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.error(
            WebClientResponseException.create(
                statusCode,
                statusText,
                null,
                null,
                null,
                null
            )
        )

        when:
        def result = pingService.sendPing().block()
        
        then:
        result.status == PingService.PingStatus.SUCCESS
        result.response.contains(statusText)
        
        where:
        statusCode | statusText
        400       | "Bad Request"
        404       | "Not Found" 
        500       | "Internal Server Error"
    }

    def "should handle connection errors"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        globalRateLimiter.tryAcquire() >> true
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.attribute("parameter", "hello") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.error(
            new RuntimeException("Connection refused")
        )

        when:
        def result = pingService.sendPing().block()
        
        then:
        result.status == PingService.PingStatus.SUCCESS
        result.response.contains("Connection refused")
    }

    def "should verify response content for successful ping"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        def response = Mock(ResponseEntity)
        globalRateLimiter.tryAcquire() >> true
        
        response.getStatusCode() >> HttpStatus.OK
        response.getBody() >> "PONG"
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.attribute("parameter", "hello") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.just(response)

        when:
        def result = pingService.sendPing().block()
        
        then:
        result.status == PingService.PingStatus.SUCCESS
        result.response == "PONG"
    }

    def "should handle timeout errors"() {
        given:
        def requestHeadersUriSpec = Mock(WebClient.RequestHeadersUriSpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)
        globalRateLimiter.tryAcquire() >> true
        
        webClient.get() >> requestHeadersUriSpec
        requestHeadersUriSpec.uri("/api/pong") >> requestHeadersSpec
        requestHeadersSpec.attribute("parameter", "hello") >> requestHeadersSpec
        requestHeadersSpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> responseSpec
        responseSpec.toEntity(String) >> Mono.error(
            new java.util.concurrent.TimeoutException("Request timeout")
        )

        when:
        def result = pingService.sendPing().block()
        
        then:
        result.status == PingService.PingStatus.SUCCESS
        result.response.contains("Request timeout")
    }

    def "should handle rate limiter initialization"() {
        given:
        def rateLimit = 2
        def duration = Duration.ofSeconds(1)

        when:
        def rateLimiter = new GlobalRateLimiter(rateLimit, duration)
        
        then:
        rateLimiter != null
        rateLimiter.tryAcquire()
        
    }
} 