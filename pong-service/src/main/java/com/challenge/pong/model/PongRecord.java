package com.challenge.pong.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pong_records")
public class PongRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String requestId;
} 