package com.loveeleve.ecology.sharding.service;

import com.loveeleve.ecology.sharding.model.Order;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HintOrderService {

    private static final RowMapper<Order> ROW_MAPPER = (rs, rowNum) -> new Order(
            rs.getLong("order_id"),
            rs.getLong("user_id"),
            rs.getLong("amount"),
            rs.getString("status")
    );

    private final JdbcTemplate jdbcTemplate;

    public HintOrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(Order order, int database, int table) {
        validateHint(database, table);
        try (HintManager hintManager = HintManager.getInstance()) {
            hintManager.addDatabaseShardingValue("t_order_hint", database);
            hintManager.addTableShardingValue("t_order_hint", table);
            return jdbcTemplate.update("""
                            INSERT INTO t_order_hint (order_id, user_id, amount, status)
                            VALUES (?, ?, ?, ?)
                            """,
                    order.orderId(), order.userId(), order.amount(), order.status());
        }
    }

    public Optional<Order> find(long orderId, int database, int table) {
        validateHint(database, table);
        try (HintManager hintManager = HintManager.getInstance()) {
            hintManager.addDatabaseShardingValue("t_order_hint", database);
            hintManager.addTableShardingValue("t_order_hint", table);
            return jdbcTemplate.query("""
                            SELECT order_id, user_id, amount, status
                            FROM t_order_hint
                            WHERE order_id = ?
                            """,
                    ROW_MAPPER, orderId).stream().findFirst();
        }
    }

    private void validateHint(int database, int table) {
        if (database < 0 || database > 1 || table < 0 || table > 1) {
            throw new IllegalArgumentException("hint database/table must be 0 or 1");
        }
    }
}
