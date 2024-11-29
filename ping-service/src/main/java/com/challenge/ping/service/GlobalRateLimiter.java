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
    // 定义存储限流数据的文件名
    private static final String DATA_FILE = "ping-rate.data";
    // 速率限制次数
    private final int rateLimit;
    // 时间窗口
    private final Duration duration;
    // 数据文件路径
    private final Path dataPath;
    
    // 默认构造函数：每秒最多2次请求
    public GlobalRateLimiter() {
        this(2, Duration.ofSeconds(1));
    }

    public GlobalRateLimiter(int rateLimit, Duration duration) {
        // 确保速率限制至少为1
        this.rateLimit = Math.max(1, rateLimit);
        this.duration = duration;
        this.dataPath = new File(DATA_FILE).toPath();
    }

    /**
     * 尝试获取请求权限
     * @return 如果成功获取请求权限，返回true；否则返回false
     */
    public boolean tryAcquire() {
        // 使用try-with-resources确保资源自动关闭
        try (
            // 打开文件通道，支持创建、读写操作
            FileChannel channel = FileChannel.open(dataPath, 
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
                
            // 获取文件锁以确保线程安全
            try (FileLock lock = channel.tryLock()) {
                // 分配16字节的缓冲区（存储两个long值）
                ByteBuffer buffer = ByteBuffer.allocate(16);
                channel.position(0);
                // 读取文件内容
                int bytesRead = channel.read(buffer);
                buffer.flip();
                
                // 声明上次请求时间和请求计数变量
                long lastRequestTime;
                long requestCount;
                
                // 如果读取了完整的数据（16字节）
                if (bytesRead == 16) {
                    // 解析已存储的数据
                    lastRequestTime = buffer.getLong();
                    requestCount = buffer.getLong();
                } else {
                    // 如果是首次使用，初始化数据
                    lastRequestTime = System.currentTimeMillis();
                    requestCount = 0;
                }
                
                // 获取当前时间
                long now = System.currentTimeMillis();
                // 如果已经超过时间窗口，重置计数器
                if (now - lastRequestTime >= duration.toMillis()) {
                    lastRequestTime = now;
                    requestCount = 0;
                }
                
                // 如果未超过速率限制
                if (requestCount < rateLimit) {
                    // 增加请求计数
                    requestCount++;
                    // 清空缓冲区
                    buffer.clear();
                    // 写入新的数据
                    buffer.putLong(lastRequestTime);
                    buffer.putLong(requestCount);
                    buffer.flip();
                    
                    // 将更新后的数据写回文件
                    channel.position(0);
                    channel.write(buffer);
                    // 强制写入磁盘
                    channel.force(true);
                    return true;
                }
                
                // 如果超过速率限制，返回false
                return false;
            }
        } catch (Exception e) {
            // 记录错误日志
            log.error("Error while trying to acquire rate limit", e);
            return false;
        }
    }

    // 清理方法：删除数据文件
    public void cleanup() {
        try {
            Files.deleteIfExists(dataPath);
        } catch (IOException e) {
            // 记录清理错误日志
            log.error("Error cleaning up rate limiter", e);
        }
    }
} 