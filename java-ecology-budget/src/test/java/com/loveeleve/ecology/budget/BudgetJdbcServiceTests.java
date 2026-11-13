package com.loveeleve.ecology.budget;

import com.loveeleve.ecology.budget.domain.BudgetFlow;
import com.loveeleve.ecology.budget.domain.BudgetFlowStatus;
import com.loveeleve.ecology.budget.domain.BudgetLease;
import com.loveeleve.ecology.budget.domain.BudgetUnit;
import com.loveeleve.ecology.budget.domain.LeaseRequest;
import com.loveeleve.ecology.budget.domain.ReserveRequest;
import com.loveeleve.ecology.budget.domain.ReserveResult;
import com.loveeleve.ecology.budget.service.BudgetJdbcService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 预扣服务生产链路集成测试：Spring Boot + JdbcTemplate + H2(MySQL 模式)。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BudgetJdbcServiceTests {

    @Autowired
    private BudgetJdbcService service;

    @Test
    void reserveIsIdempotentAndConfirmOnlyOnce() {
        String requestId = "test-" + UUID.randomUUID();
        ReserveRequest request = new ReserveRequest(
                requestId, "U-test", "A1", "T1", BudgetUnit.AMOUNT,
                new BigDecimal("20"), List.of("market", "product"));

        ReserveResult first = service.reserve(request);
        ReserveResult replay = service.reserve(request);

        assertThat(replay.txnId()).isEqualTo(first.txnId());
        assertThat(first.sourceId()).isEqualTo("market");

        BudgetFlow confirmed = service.confirm(first.txnId());
        BudgetFlow confirmedAgain = service.confirm(first.txnId());

        assertThat(confirmed.status()).isEqualTo(BudgetFlowStatus.CONFIRMED);
        assertThat(confirmedAgain.status()).isEqualTo(BudgetFlowStatus.CONFIRMED);
    }

    @Test
    void leaseReserveConfirmAndReturn() {
        String leaseRequestId = "lease-" + UUID.randomUUID();
        BudgetLease lease = service.allocateLease(new LeaseRequest(
                leaseRequestId, "instance-test", "A1", "market", "T1",
                BudgetUnit.AMOUNT, new BigDecimal("100"),
                Instant.now().plus(Duration.ofMinutes(2))));

        ReserveRequest request = new ReserveRequest(
                "lease-reserve-" + UUID.randomUUID(), "U-lease", "A1", "T1",
                BudgetUnit.AMOUNT, new BigDecimal("30"), List.of("market"));

        BudgetFlow flow = service.reserveFromLease(request, lease.leaseId());
        assertThat(flow.leaseId()).isEqualTo(lease.leaseId());

        service.confirm(flow.txnId());
        service.returnLease(lease.leaseId());

        BudgetLease returned = service.findLeaseById(lease.leaseId());
        assertThat(returned.remaining()).isEqualByComparingTo("70");
    }

    @Test
    void requestIdWithDifferentAmountIsConflict() {
        String requestId = "conflict-" + UUID.randomUUID();
        service.reserve(new ReserveRequest(
                requestId, "U-conflict", "A1", "T1", BudgetUnit.AMOUNT,
                new BigDecimal("20"), List.of("market")));

        ReserveRequest different = new ReserveRequest(
                requestId, "U-conflict", "A1", "T1", BudgetUnit.AMOUNT,
                new BigDecimal("50"), List.of("market"));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.loveeleve.ecology.budget.exception.BudgetException.class,
                () -> service.reserve(different));
    }
}
