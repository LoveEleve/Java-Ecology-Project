package com.loveeleve.ecology.seata.account;

import com.loveeleve.ecology.seata.account.service.AccountService;
import com.loveeleve.ecology.seata.account.service.ConcurrentRequestException;
import com.loveeleve.ecology.seata.account.service.IdempotencyConflictException;
import com.loveeleve.ecology.seata.account.service.InsufficientBalanceException;
import com.loveeleve.ecology.seata.api.AccountDebitRequest;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountServiceTests {

    private JdbcTemplate jdbcTemplate;
    private AccountService accountService;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:seata-account-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP ALL OBJECTS");
        executeScript("src/main/resources/db/schema-h2.sql");
        executeScript("src/main/resources/db/data-h2.sql");
        accountService = new AccountService(jdbcTemplate);
    }

    @Test
    void debitMovesBalanceAndMarksRequestSuccessful() {
        accountService.debit(new AccountDebitRequest("acc-1", 1L, new BigDecimal("100.00")));

        assertThat(balanceOf(1L)).isEqualByComparingTo("900.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM account_txn WHERE request_id = ?", String.class, "acc-1"))
                .isEqualTo("SUCCESS");
    }

    @Test
    void duplicateSuccessfulRequestDoesNotDebitTwice() {
        AccountDebitRequest request = new AccountDebitRequest("acc-2", 1L, new BigDecimal("100.00"));
        accountService.debit(request);
        accountService.debit(request);

        assertThat(balanceOf(1L)).isEqualByComparingTo("900.00");
    }

    @Test
    void reusingRequestIdWithDifferentPayloadIsRejected() {
        accountService.debit(new AccountDebitRequest("acc-5", 1L, new BigDecimal("100.00")));

        assertThatThrownBy(() -> accountService.debit(
                new AccountDebitRequest("acc-5", 1L, new BigDecimal("50.00"))))
                .isInstanceOf(IdempotencyConflictException.class);
        assertThat(balanceOf(1L)).isEqualByComparingTo("900.00");
    }

    @Test
    void insufficientBalanceIsRejected() {
        assertThatThrownBy(() -> accountService.debit(
                new AccountDebitRequest("acc-3", 1L, new BigDecimal("2000.00"))))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(balanceOf(1L)).isEqualByComparingTo("1000.00");
    }

    @Test
    void processingRequestIsRejectedToAvoidDuplicateDebit() {
        jdbcTemplate.update("""
                        INSERT INTO account_txn (request_id, user_id, amount, status, created_at)
                        VALUES ('acc-4', 1, 100.00, 'PROCESSING', CURRENT_TIMESTAMP)
                        """);

        assertThatThrownBy(() -> accountService.debit(
                new AccountDebitRequest("acc-4", 1L, new BigDecimal("100.00"))))
                .isInstanceOf(ConcurrentRequestException.class);
    }

    private BigDecimal balanceOf(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account_tbl WHERE user_id = ?", BigDecimal.class, userId);
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
