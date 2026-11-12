package com.loveeleve.ecology.sharding.repository;

import com.loveeleve.ecology.sharding.model.OrderItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderItemRepository {

    private static final RowMapper<OrderItem> ROW_MAPPER = (rs, rowNum) -> new OrderItem(
            rs.getLong("item_id"),
            rs.getLong("order_id"),
            rs.getLong("user_id"),
            rs.getString("product_name"),
            rs.getInt("quantity")
    );

    private final JdbcTemplate jdbcTemplate;

    public OrderItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(OrderItem item) {
        return jdbcTemplate.update("""
                        INSERT INTO t_order_item (item_id, order_id, user_id, product_name, quantity)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                item.itemId(), item.orderId(), item.userId(), item.productName(), item.quantity());
    }

    public Optional<OrderItem> findByOrderAndUser(long orderId, long userId) {
        return jdbcTemplate.query("""
                        SELECT item_id, order_id, user_id, product_name, quantity
                        FROM t_order_item
                        WHERE order_id = ? AND user_id = ?
                        ORDER BY item_id
                        """,
                ROW_MAPPER, orderId, userId).stream().findFirst();
    }

    public List<OrderItem> findByOrderId(long orderId, long userId) {
        return jdbcTemplate.query("""
                        SELECT item_id, order_id, user_id, product_name, quantity
                        FROM t_order_item
                        WHERE order_id = ? AND user_id = ?
                        ORDER BY item_id
                        """,
                ROW_MAPPER, orderId, userId);
    }

    public Optional<OrderItem> findByItemId(long itemId) {
        return jdbcTemplate.query("""
                        SELECT item_id, order_id, user_id, product_name, quantity
                        FROM t_order_item
                        WHERE item_id = ?
                        ORDER BY item_id
                        """,
                ROW_MAPPER, itemId).stream().findFirst();
    }
}
