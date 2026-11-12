package com.loveeleve.ecology.seata.storage;

import com.loveeleve.ecology.seata.api.StorageDeductRequest;
import com.loveeleve.ecology.seata.storage.service.ConcurrentStorageRequestException;
import com.loveeleve.ecology.seata.storage.service.IdempotencyConflictException;
import com.loveeleve.ecology.seata.storage.service.InsufficientStockException;
import com.loveeleve.ecology.seata.storage.service.StorageService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceTests {

    private JdbcTemplate jdbcTemplate;
    private StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:seata-storage-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP ALL OBJECTS");
        executeScript("src/main/resources/db/schema-h2.sql");
        executeScript("src/main/resources/db/data-h2.sql");
        storageService = new StorageService(jdbcTemplate);
    }

    @Test
    void deductMovesStockAndMarksRequestSuccessful() {
        storageService.deduct(new StorageDeductRequest("sto-1", 1L, 2));

        assertThat(stockOf(1L)).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM storage_txn WHERE request_id = ?", String.class, "sto-1"))
                .isEqualTo("SUCCESS");
    }

    @Test
    void duplicateSuccessfulRequestDoesNotDeductTwice() {
        StorageDeductRequest request = new StorageDeductRequest("sto-2", 1L, 2);
        storageService.deduct(request);
        storageService.deduct(request);

        assertThat(stockOf(1L)).isEqualTo(8);
    }

    @Test
    void reusingRequestIdWithDifferentPayloadIsRejected() {
        storageService.deduct(new StorageDeductRequest("sto-5", 1L, 2));

        assertThatThrownBy(() -> storageService.deduct(new StorageDeductRequest("sto-5", 1L, 3)))
                .isInstanceOf(IdempotencyConflictException.class);
        assertThat(stockOf(1L)).isEqualTo(8);
    }

    @Test
    void insufficientStockIsRejected() {
        assertThatThrownBy(() -> storageService.deduct(new StorageDeductRequest("sto-3", 1L, 20)))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(stockOf(1L)).isEqualTo(10);
    }

    @Test
    void processingRequestIsRejectedToAvoidDuplicateDeduct() {
        jdbcTemplate.update("""
                        INSERT INTO storage_txn (request_id, product_id, quantity, status, created_at)
                        VALUES ('sto-4', 1, 2, 'PROCESSING', CURRENT_TIMESTAMP)
                        """);

        assertThatThrownBy(() -> storageService.deduct(new StorageDeductRequest("sto-4", 1L, 2)))
                .isInstanceOf(ConcurrentStorageRequestException.class);
    }

    private int stockOf(long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT stock FROM storage_tbl WHERE product_id = ?", Integer.class, productId);
    }

    private void executeScript(String path) throws Exception {
        String schema = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        for (String sql : schema.split(";")) {
            if (!sql.isBlank()) {
                jdbcTemplate.execute(sql);
            }
        }
    }
}
