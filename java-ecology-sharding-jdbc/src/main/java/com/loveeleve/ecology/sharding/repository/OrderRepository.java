package com.loveeleve.ecology.sharding.repository;

import com.loveeleve.ecology.sharding.exception.OrderNotFoundException;
import com.loveeleve.ecology.sharding.model.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OrderRepository {

    private static final RowMapper<Order> ROW_MAPPER = (rs, rowNum) -> new Order(
            rs.getLong("order_id"),
            rs.getLong("user_id"),
            rs.getLong("amount"),
            rs.getString("status")
    );

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(Order order) {
        return jdbcTemplate.update("""
                        INSERT INTO t_order (order_id, user_id, amount, status)
                        VALUES (?, ?, ?, ?)
                        """,
                order.orderId(), order.userId(), order.amount(), order.status());
    }

    public Optional<Order> find(long orderId, long userId) {
        return jdbcTemplate.query("""
                        SELECT order_id, user_id, amount, status
                        FROM t_order
                        WHERE order_id = ? AND user_id = ?
                        """,
                ROW_MAPPER, orderId, userId).stream().findFirst();
    }

    public java.util.List<Order> findByUser(long userId) {
        return jdbcTemplate.query("""
                        SELECT order_id, user_id, amount, status
                        FROM t_order
                        WHERE user_id = ?
                        ORDER BY order_id
                        """,
                ROW_MAPPER, userId);
    }

    public void updateStatus(long orderId, long userId, String status) {
        int updated = jdbcTemplate.update("""
                        UPDATE t_order
                        SET status = ?
                        WHERE order_id = ? AND user_id = ?
                        """,
                status, orderId, userId);
        if (updated == 0) {
            throw new OrderNotFoundException(orderId);
        }
    }
}
