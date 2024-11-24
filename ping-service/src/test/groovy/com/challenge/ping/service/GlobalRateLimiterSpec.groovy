package com.challenge.ping.service

import spock.lang.Specification
import java.time.Duration
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GlobalRateLimiterSpec extends Specification {
    
    def cleanup() {
        // 每个测试后清理文件
        new File("ping-rate.data").delete()
    }
    
    def "should create rate limiter with default constructor"() {
        when:
        def rateLimiter = new GlobalRateLimiter()
        
        then:
        rateLimiter != null
        
        cleanup:
        rateLimiter.cleanup()
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
    
    
    def "should handle data file corruption"() {
        given:
        def rateLimiter = new GlobalRateLimiter(1, Duration.ofSeconds(1))
        def dataFile = new File("ping-rate.data")
        dataFile.bytes = [1, 2, 3] // 写入无效数据
        
        when:
        def result = rateLimiter.tryAcquire()
        
        then:
        result // 应该重置并允许请求
        
        cleanup:
        rateLimiter.cleanup()
    }
    
    def "should handle cleanup with missing files"() {
        given:
        def rateLimiter = new GlobalRateLimiter(1, Duration.ofSeconds(1))
        new File("ping-rate.data").delete()
        
        when:
        rateLimiter.cleanup()
        
        then:
        noExceptionThrown()
    }
    
    def "should handle rate limit reset after duration"() {
        given:
        def rateLimiter = new GlobalRateLimiter(1, Duration.ofMillis(100))
        
        when:
        def result1 = rateLimiter.tryAcquire()
        Thread.sleep(150) // 等待超过限制时间
        def result2 = rateLimiter.tryAcquire()
        
        then:
        result1
        result2
        
        cleanup:
        rateLimiter.cleanup()
    }
    
    def "should handle concurrent access"() {
        given:
        def rateLimiter = new GlobalRateLimiter(2, Duration.ofSeconds(1))
        def results = Collections.synchronizedList([])
        def startLatch = new CountDownLatch(1)
        def completionLatch = new CountDownLatch(5)
        def threads = []
        
        when:
        // 创建5个线程
        5.times { threadId ->
            threads << Thread.start {
                try {
                    // 等待开始信号
                    startLatch.await(5, TimeUnit.SECONDS)
                    // 尝试获取限流许可
                    synchronized(results) {
                        results.add(rateLimiter.tryAcquire())
                    }
                } catch (Exception e) {
                    log.error("Error in thread ${threadId}", e)
                } finally {
                    completionLatch.countDown()
                }
            }
        }
        
        // 发出开始信号
        Thread.sleep(100) // 确保所有线程都已准备就绪
        startLatch.countDown()
        
        // 等待所有线程完成
        assert completionLatch.await(5, TimeUnit.SECONDS)
        
        then:
        results.size() == 5
        results.count { it } == 2  // 应该有2个成功
        results.count { !it } == 3 // 应该有3个失败
        
        cleanup:
        rateLimiter.cleanup()
        threads.each { thread ->
            try {
                thread.join(1000)
                if (thread.isAlive()) {
                    thread.interrupt()
                }
            } catch (InterruptedException e) {
                log.error("Error cleaning up thread", e)
            }
        }
    }
    
    def "should handle invalid rate limit parameters"() {
        when:
        def rateLimiter = new GlobalRateLimiter(rateLimit, duration)
        
        then:
        noExceptionThrown()
        
        cleanup:
        rateLimiter.cleanup()
        
        where:
        rateLimit | duration
        0         | Duration.ofSeconds(1)
        -1        | Duration.ofSeconds(1)
        1         | Duration.ofMillis(0)
        1         | Duration.ofMillis(-1)
    }
    
} 