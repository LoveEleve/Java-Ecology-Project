package com.loveeleve.ecology.druid.repository;

import com.loveeleve.ecology.druid.model.PoolUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class PoolUserRepository {

    private static final RowMapper<PoolUser> ROW_MAPPER = (rs, rowNum) -> new PoolUser(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getObject("age", Integer.class),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public PoolUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PoolUser insert(String username, String email, Integer age) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO ecology_user (username, email, age, created_at) VALUES (?, ?, ?, ?)",
                    new String[]{"id"});
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setObject(3, age);
            ps.setObject(4, LocalDateTime.now());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("insert did not return a generated key");
        }
        return findById(key.longValue()).orElseThrow();
    }

    public Optional<PoolUser> findById(long id) {
        return jdbcTemplate.query("SELECT id, username, email, age, created_at FROM ecology_user WHERE id = ?",
                ROW_MAPPER, id).stream().findFirst();
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ecology_user", Long.class);
        return count == null ? 0 : count;
    }
}
