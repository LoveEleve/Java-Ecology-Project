package com.loveeleve.ecology.rocketmq.service;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedOrderStoreTests {

    private ProcessedOrderStore store;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:rocketmq-idempotency;MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP ALL OBJECTS");
        String schema = Files.readString(
                Path.of("src/main/resources/db/schema-h2.sql"), StandardCharsets.UTF_8);
        for (String sql : schema.split(";")) {
            if (!sql.isBlank()) {
                jdbcTemplate.execute(sql);
            }
        }
        store = new ProcessedOrderStore(jdbcTemplate);
    }

    @Test
    void duplicateMessageIsClaimedOnlyOnce() {
        assertThat(store.tryClaim("rmq-1")).isEqualTo(ProcessedOrderStore.ClaimResult.CLAIMED);
        store.markSuccess("rmq-1");

        assertThat(store.tryClaim("rmq-1")).isEqualTo(ProcessedOrderStore.ClaimResult.SUCCESS);
        assertThat(store.processedCount()).isEqualTo(1);
    }

    @Test
    void inProgressMessageIsRetriedInsteadOfAcknowledged() {
        assertThat(store.tryClaim("rmq-3")).isEqualTo(ProcessedOrderStore.ClaimResult.CLAIMED);

        assertThat(store.tryClaim("rmq-3")).isEqualTo(ProcessedOrderStore.ClaimResult.IN_PROGRESS);
    }

    @Test
    void staleProcessingClaimCanBeReclaimed() {
        assertThat(store.tryClaim("rmq-4")).isEqualTo(ProcessedOrderStore.ClaimResult.CLAIMED);
        jdbcTemplate.update("""
                        UPDATE rocketmq_processed_order
                        SET updated_at = ?
                        WHERE order_id = ?
                        """,
                LocalDateTime.now().minusSeconds(31), "rmq-4");

        assertThat(store.tryClaim("rmq-4")).isEqualTo(ProcessedOrderStore.ClaimResult.CLAIMED);
    }

    @Test
    void failedMessageCanBeRetried() {
        assertThat(store.tryClaim("rmq-2")).isEqualTo(ProcessedOrderStore.ClaimResult.CLAIMED);
        store.markFailed("rmq-2");

        assertThat(store.tryClaim("rmq-2")).isEqualTo(ProcessedOrderStore.ClaimResult.CLAIMED);
        store.markSuccess("rmq-2");

        assertThat(store.processedCount()).isEqualTo(1);
    }
}
