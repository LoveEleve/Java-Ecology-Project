package com.loveeleve.ecology.rocketmq.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ProcessedOrderStore {

    private static final Duration CLAIM_LEASE = Duration.ofSeconds(30);

    private final JdbcTemplate jdbcTemplate;

    public ProcessedOrderStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ClaimResult tryClaim(String orderId) {
        LocalDateTime now = LocalDateTime.now();
        try {
            jdbcTemplate.update("""
                            INSERT INTO rocketmq_processed_order (order_id, status, processed_at, updated_at)
                            VALUES (?, 'PROCESSING', NULL, ?)
                            """,
                    orderId, now);
            return ClaimResult.CLAIMED;
        } catch (DuplicateKeyException ex) {
            ProcessedState state = jdbcTemplate.queryForObject("""
                            SELECT status, updated_at
                            FROM rocketmq_processed_order
                            WHERE order_id = ?
                            """,
                    (rs, rowNum) -> new ProcessedState(
                            rs.getString("status"),
                            rs.getTimestamp("updated_at").toLocalDateTime()),
                    orderId);

            if (state == null || "SUCCESS".equals(state.status())) {
                return ClaimResult.SUCCESS;
            }
            if ("PROCESSING".equals(state.status())
                    && state.updatedAt().isAfter(now.minus(CLAIM_LEASE))) {
                return ClaimResult.IN_PROGRESS;
            }
            int updated = jdbcTemplate.update("""
                            UPDATE rocketmq_processed_order
                            SET status = 'PROCESSING', updated_at = ?
                            WHERE order_id = ? AND status = ? AND updated_at = ?
                            """,
                    now, orderId, state.status(), state.updatedAt());
            return updated == 1 ? ClaimResult.CLAIMED : ClaimResult.IN_PROGRESS;
        }
    }

    public void markSuccess(String orderId) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                        UPDATE rocketmq_processed_order
                        SET status = 'SUCCESS', processed_at = ?, updated_at = ?
                        WHERE order_id = ?
                        """,
                now, now, orderId);
    }

    public void markFailed(String orderId) {
        jdbcTemplate.update("""
                        UPDATE rocketmq_processed_order
                        SET status = 'FAILED', updated_at = ?
                        WHERE order_id = ?
                        """,
                LocalDateTime.now(), orderId);
    }

    public long processedCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rocketmq_processed_order WHERE status = 'SUCCESS'", Long.class);
        return count == null ? 0 : count;
    }

    public enum ClaimResult {
        CLAIMED,
        IN_PROGRESS,
        SUCCESS
    }

    private record ProcessedState(String status, LocalDateTime updatedAt) {
    }
}
