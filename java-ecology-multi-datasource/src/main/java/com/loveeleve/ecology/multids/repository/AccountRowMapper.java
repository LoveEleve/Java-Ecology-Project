package com.loveeleve.ecology.multids.repository;

import com.loveeleve.ecology.multids.model.Account;
import org.springframework.jdbc.core.RowMapper;

public final class AccountRowMapper implements RowMapper<Account> {

    public static final AccountRowMapper INSTANCE = new AccountRowMapper();

    private AccountRowMapper() {
    }

    @Override
    public Account mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Account(rs.getLong("id"), rs.getString("owner"), rs.getLong("balance"));
    }
}
