package com.challenge.ping.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PingRecord {
    private Long id;
    
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String requestId;
} 