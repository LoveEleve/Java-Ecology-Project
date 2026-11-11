package com.loveeleve.ecology.multids.repository;

import com.loveeleve.ecology.multids.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DynamicAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public DynamicAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Account> findAll() {
        return jdbcTemplate.query("SELECT id, owner, balance FROM account ORDER BY id", AccountRowMapper.INSTANCE);
    }
}
