package com.loveeleve.ecology.seata.storage.service;

import com.loveeleve.ecology.seata.api.StorageDeductRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class StorageService {

    private final JdbcTemplate jdbcTemplate;

    public StorageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void deduct(StorageDeductRequest request) {
        validate(request);
        try {
            jdbcTemplate.update("""
                            INSERT INTO storage_txn (request_id, product_id, quantity, status, created_at)
                            VALUES (?, ?, ?, 'PROCESSING', ?)
                            """,
                    request.requestId(), request.productId(), request.quantity(), LocalDateTime.now());
        } catch (DuplicateKeyException ex) {
            StorageTxn txn = jdbcTemplate.queryForObject("""
                            SELECT product_id, quantity, status FROM storage_txn WHERE request_id = ?
                            """,
                    (rs, rowNum) -> new StorageTxn(
                            rs.getLong("product_id"),
                            rs.getInt("quantity"),
                            rs.getString("status")),
                    request.requestId());
            if (txn != null && "SUCCESS".equals(txn.status())) {
                if (txn.productId() != request.productId()
                        || txn.quantity() != request.quantity()) {
                    throw new IdempotencyConflictException("requestId was used with another payload");
                }
                return;
            }
            throw new ConcurrentStorageRequestException("storage request is being processed");
        }

        int updated = jdbcTemplate.update("""
                        UPDATE storage_tbl
                        SET stock = stock - ?, updated_at = ?
                        WHERE product_id = ? AND stock >= ?
                        """,
                request.quantity(), LocalDateTime.now(), request.productId(), request.quantity());
        if (updated != 1) {
            throw new InsufficientStockException("insufficient stock for product " + request.productId());
        }
        jdbcTemplate.update("""
                        UPDATE storage_txn
                        SET status = 'SUCCESS'
                        WHERE request_id = ?
                        """,
                request.requestId());
    }

    private record StorageTxn(long productId, int quantity, String status) {
    }

    private void validate(StorageDeductRequest request) {
        if (request == null || !StringUtils.hasText(request.requestId())
                || request.productId() == null || request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("invalid storage deduct request");
        }
    }
}
