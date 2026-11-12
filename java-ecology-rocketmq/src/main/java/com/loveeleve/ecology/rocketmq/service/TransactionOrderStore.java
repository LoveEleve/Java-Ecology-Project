package com.loveeleve.ecology.rocketmq.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TransactionOrderStore {

    private final JdbcTemplate jdbcTemplate;

    public TransactionOrderStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void mark(String orderId, String status) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO rocketmq_transaction_order (order_id, status, updated_at)
                            VALUES (?, ?, ?)
                            """,
                    orderId, status, LocalDateTime.now());
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update("""
                            UPDATE rocketmq_transaction_order
                            SET status = ?, updated_at = ?
                            WHERE order_id = ?
                            """,
                    status, LocalDateTime.now(), orderId);
        }
    }

    public Optional<String> status(String orderId) {
        return jdbcTemplate.query(
                        "SELECT status FROM rocketmq_transaction_order WHERE order_id = ?",
                        (rs, rowNum) -> rs.getString("status"), orderId)
                .stream().findFirst();
    }
}
