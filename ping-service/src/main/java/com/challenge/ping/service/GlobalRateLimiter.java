package com.challenge.ping.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.io.File;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.nio.file.Files;

@Slf4j
@Component
public class GlobalRateLimiter {
    private static final String DATA_FILE = "ping-rate.data";
    private final int rateLimit;
    private final Duration duration;
    private final Path dataPath;
    
    public GlobalRateLimiter() {
        this(2, Duration.ofSeconds(1));
    }

    public GlobalRateLimiter(int rateLimit, Duration duration) {
        this.rateLimit = Math.max(1, rateLimit);
        this.duration = duration;
        this.dataPath = new File(DATA_FILE).toPath();
    }

    public boolean tryAcquire() {
        try (FileChannel channel = FileChannel.open(dataPath, 
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
                
            try (FileLock lock = channel.tryLock()) {
                ByteBuffer buffer = ByteBuffer.allocate(16);
                channel.position(0);
                int bytesRead = channel.read(buffer);
                buffer.flip();
                
                long lastRequestTime;
                long requestCount;
                
                if (bytesRead == 16) {
                    lastRequestTime = buffer.getLong();
                    requestCount = buffer.getLong();
                } else {
                    lastRequestTime = System.currentTimeMillis();
                    requestCount = 0;
                }
                
                long now = System.currentTimeMillis();
                if (now - lastRequestTime >= duration.toMillis()) {
                    lastRequestTime = now;
                    requestCount = 0;
                }
                
                if (requestCount < rateLimit) {
                    requestCount++;
                    buffer.clear();
                    buffer.putLong(lastRequestTime);
                    buffer.putLong(requestCount);
                    buffer.flip();
                    
                    channel.position(0);
                    channel.write(buffer);
                    channel.force(true);
                    return true;
                }
                
                return false;
            }
        } catch (Exception e) {
            log.error("Error while trying to acquire rate limit", e);
            return false;
        }
    }

    public void cleanup() {
        try {
            Files.deleteIfExists(dataPath);
        } catch (IOException e) {
            log.error("Error cleaning up rate limiter", e);
        }
    }
} 