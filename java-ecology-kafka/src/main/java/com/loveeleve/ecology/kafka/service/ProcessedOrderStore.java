package com.loveeleve.ecology.kafka.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProcessedOrderStore {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedOrderStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean markProcessedIfAbsent(String orderId) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO kafka_processed_order (order_id, processed_at)
                            VALUES (?, ?)
                            """,
                    orderId, LocalDateTime.now());
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public boolean contains(String orderId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kafka_processed_order WHERE order_id = ?", Integer.class, orderId);
        return count != null && count > 0;
    }

    public void markFailed(String orderId) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO kafka_failed_order (order_id, failed_at)
                            VALUES (?, ?)
                            """,
                    orderId, LocalDateTime.now());
        } catch (DuplicateKeyException ignored) {
            // already recorded
        }
    }

    public long processedCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kafka_processed_order", Long.class);
        return count == null ? 0 : count;
    }

    public long failedCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kafka_failed_order", Long.class);
        return count == null ? 0 : count;
    }
}
