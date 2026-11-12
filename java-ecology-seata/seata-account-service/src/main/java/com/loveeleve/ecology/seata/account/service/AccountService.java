package com.loveeleve.ecology.seata.account.service;

import com.loveeleve.ecology.seata.api.AccountDebitRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AccountService {

    private final JdbcTemplate jdbcTemplate;

    public AccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void debit(AccountDebitRequest request) {
        validate(request);
        try {
            jdbcTemplate.update("""
                            INSERT INTO account_txn (request_id, user_id, amount, status, created_at)
                            VALUES (?, ?, ?, 'PROCESSING', ?)
                            """,
                    request.requestId(), request.userId(), request.amount(), LocalDateTime.now());
        } catch (DuplicateKeyException ex) {
            AccountTxn txn = jdbcTemplate.queryForObject("""
                            SELECT user_id, amount, status FROM account_txn WHERE request_id = ?
                            """,
                    (rs, rowNum) -> new AccountTxn(
                            rs.getLong("user_id"),
                            rs.getBigDecimal("amount"),
                            rs.getString("status")),
                    request.requestId());
            if (txn != null && "SUCCESS".equals(txn.status())) {
                if (txn.userId() != request.userId()
                        || txn.amount().compareTo(request.amount()) != 0) {
                    throw new IdempotencyConflictException("requestId was used with another payload");
                }
                return;
            }
            throw new ConcurrentRequestException("account request is being processed");
        }

        int updated = jdbcTemplate.update("""
                        UPDATE account_tbl
                        SET balance = balance - ?, updated_at = ?
                        WHERE user_id = ? AND balance >= ?
                        """,
                request.amount(), LocalDateTime.now(), request.userId(), request.amount());
        if (updated != 1) {
            throw new InsufficientBalanceException("insufficient balance for user " + request.userId());
        }
        jdbcTemplate.update("""
                        UPDATE account_txn
                        SET status = 'SUCCESS'
                        WHERE request_id = ?
                        """,
                request.requestId());
    }

    private record AccountTxn(long userId, BigDecimal amount, String status) {
    }

    private void validate(AccountDebitRequest request) {
        if (request == null || !StringUtils.hasText(request.requestId())
                || request.userId() == null || request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("invalid account debit request");
        }
    }
}
