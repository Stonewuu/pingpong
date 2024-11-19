package com.challenge.ping.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.io.File;

@Slf4j
@Component
public class GlobalRateLimiter {
    private static final String LOCK_FILE = "ping-rate.lock";
    private static final String DATA_FILE = "ping-rate.data";
    private final double rate;
    private final long intervalMs;
    
    private final RandomAccessFile dataFile;
    private final FileChannel channel;
    
    public GlobalRateLimiter() throws IOException {
        this(2, Duration.ofSeconds(1));
    }
    
    public GlobalRateLimiter(Integer rate, Duration interval) throws IOException {
        this.rate = rate;
        this.intervalMs = interval.toMillis();
        this.dataFile = new RandomAccessFile(DATA_FILE, "rw");
        this.channel = dataFile.getChannel();
        
        if (dataFile.length() == 0) {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(0);
            buffer.putLong(0);
            buffer.flip();
            channel.write(buffer);
        }
    }
    
    public boolean tryAcquire() {
        try (RandomAccessFile lockFile = new RandomAccessFile(LOCK_FILE, "rw");
             FileChannel lockChannel = lockFile.getChannel();
             FileLock lock = lockChannel.lock()) {
            
            // 读取当前状态
            ByteBuffer buffer = ByteBuffer.allocate(16);
            channel.position(0);
            channel.read(buffer);
            buffer.flip();
            
            long lastRequestTime = buffer.getLong();
            long requestCount = buffer.getLong();
            long now = Instant.now().toEpochMilli();
            
            // 检查是否需要重置时间窗口
            if (now - lastRequestTime >= intervalMs) {
                lastRequestTime = now;
                requestCount = 0;
            }
            
            // 检查是否可以发送请求
            if (requestCount < rate) {
                requestCount++;
                
                // 更新状态
                buffer.clear();
                buffer.putLong(lastRequestTime);
                buffer.putLong(requestCount);
                buffer.flip();
                channel.position(0);
                channel.write(buffer);
                
                return true;
            }
            
            return false;
        } catch (IOException e) {
            log.error("Error while trying to acquire rate limit", e);
            return false;
        }
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            channel.close();
            dataFile.close();
            // 删除数据文件
            new File(DATA_FILE).delete();
            new File(LOCK_FILE).delete();
        } catch (IOException e) {
            log.error("Error while cleaning up rate limiter", e);
        }
    }
} 