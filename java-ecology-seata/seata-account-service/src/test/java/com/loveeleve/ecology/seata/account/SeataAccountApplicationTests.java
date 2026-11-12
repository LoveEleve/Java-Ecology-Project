package com.loveeleve.ecology.seata.account;

import com.loveeleve.ecology.seata.account.service.AccountService;
import com.loveeleve.ecology.seata.account.service.InsufficientBalanceException;
import com.loveeleve.ecology.seata.api.AccountDebitRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"seata.enabled=false", "spring.cloud.nacos.discovery.enabled=false"})
class SeataAccountApplicationTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void insufficientBalanceRollsBackLocalTransaction() {
        assertThatThrownBy(() -> accountService.debit(
                new AccountDebitRequest("ctx-rollback", 1L, new BigDecimal("2000.00"))))
                .isInstanceOf(InsufficientBalanceException.class);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_txn WHERE request_id = 'ctx-rollback'", Integer.class);
        assertThat(count).isZero();
    }
}
