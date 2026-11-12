package com.loveeleve.ecology.rocketmq.service;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionOrderStoreTests {

    private TransactionOrderStore store;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:rocketmq-transaction;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP ALL OBJECTS");
        String schema = Files.readString(
                Path.of("src/main/resources/db/schema-h2.sql"), StandardCharsets.UTF_8);
        for (String sql : schema.split(";")) {
            if (!sql.isBlank()) {
                jdbcTemplate.execute(sql);
            }
        }
        store = new TransactionOrderStore(jdbcTemplate);
    }

    @Test
    void markIsIdempotentAndStatusCanBeRead() {
        store.mark("rmq-tx-1", "COMMITTED");
        store.mark("rmq-tx-1", "COMMITTED");

        assertThat(store.status("rmq-tx-1")).contains("COMMITTED");
        assertThat(store.status("missing")).isEmpty();
    }
}
