package com.loveeleve.ecology.multids.repository;

import com.loveeleve.ecology.multids.model.Account;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MasterAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public MasterAccountRepository(@Qualifier("masterJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Account> findAll() {
        return jdbcTemplate.query("SELECT id, owner, balance FROM account ORDER BY id", AccountRowMapper.INSTANCE);
    }
}
