package com.challenge.pong.repository;

import com.challenge.pong.model.PongRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PongRecordRepository extends JpaRepository<PongRecord, Long> {
} 