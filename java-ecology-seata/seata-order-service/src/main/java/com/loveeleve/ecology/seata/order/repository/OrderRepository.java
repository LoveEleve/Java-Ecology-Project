package com.loveeleve.ecology.seata.order.repository;

import com.loveeleve.ecology.seata.api.CreateOrderRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<OrderRecord> findByRequestId(String requestId) {
        return jdbcTemplate.query("""
                        SELECT id, status, user_id, product_id, amount, quantity
                        FROM order_tbl WHERE request_id = ?
                        """,
                (rs, rowNum) -> new OrderRecord(
                        rs.getLong("id"),
                        rs.getString("status"),
                        rs.getLong("user_id"),
                        rs.getLong("product_id"),
                        rs.getBigDecimal("amount"),
                        rs.getInt("quantity")),
                requestId).stream().findFirst();
    }

    public long insert(CreateOrderRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            INSERT INTO order_tbl
                                (request_id, user_id, product_id, amount, quantity, status, created_at)
                            VALUES (?, ?, ?, ?, ?, 'CREATED', ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.requestId());
            ps.setLong(2, request.userId());
            ps.setLong(3, request.productId());
            ps.setBigDecimal(4, request.amount());
            ps.setInt(5, request.quantity());
            ps.setObject(6, LocalDateTime.now());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("order id was not generated");
        }
        return key.longValue();
    }

    public void updateStatus(long orderId, String status) {
        jdbcTemplate.update("""
                        UPDATE order_tbl SET status = ?, updated_at = ? WHERE id = ?
                        """,
                status, LocalDateTime.now(), orderId);
    }
}
