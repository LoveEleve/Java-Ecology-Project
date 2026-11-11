package com.loveeleve.ecology.multids.repository;

import com.loveeleve.ecology.multids.model.Account;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReportAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReportAccountRepository(@Qualifier("reportJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Account> findAll() {
        return jdbcTemplate.query("SELECT id, owner, balance FROM account ORDER BY id", AccountRowMapper.INSTANCE);
    }
}
