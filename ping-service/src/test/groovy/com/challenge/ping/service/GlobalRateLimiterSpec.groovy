import spock.lang.Specification
import java.time.Duration
import com.challenge.ping.service.GlobalRateLimiter
import com.challenge.ping.service.PingService

class GlobalRateLimiterSpec extends Specification {
    
    def cleanup() {
        // 每个测试后清理文件
        new File("ping-rate.lock").delete()
        new File("ping-rate.data").delete()
    }
    
    def "should allow requests within rate limit"() {
        given:
        def rateLimiter = new GlobalRateLimiter(2, Duration.ofSeconds(1))
        
        when:
        def result1 = rateLimiter.tryAcquire()
        def result2 = rateLimiter.tryAcquire()
        
        then:
        result1
        result2
        
        cleanup:
        rateLimiter.cleanup()
    }
    
    def "should reject requests exceeding rate limit"() {
        given:
        def rateLimiter = new GlobalRateLimiter(1, Duration.ofSeconds(1))
        
        when:
        def result1 = rateLimiter.tryAcquire()
        def result2 = rateLimiter.tryAcquire()
        
        then:
        result1
        !result2
        
        cleanup:
        rateLimiter.cleanup()
    }
} 