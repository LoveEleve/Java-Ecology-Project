package com.loveeleve.ecology.budget.config;

import com.loveeleve.ecology.budget.service.BudgetJdbcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 预算预占超时回收任务。
 *
 * <p>生产上建议换成 XXL-Job/中央调度，避免每个实例各扫一遍；
 * 本示例用 Spring Scheduling 表达调度位置和幂等语义。</p>
 */
@Component
public class BudgetExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BudgetExpiryJob.class);

    private final BudgetJdbcService budgetService;
    private final BudgetProperties properties;

    public BudgetExpiryJob(BudgetJdbcService budgetService, BudgetProperties properties) {
        this.budgetService = budgetService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${budget.expiry-scan-interval-ms:30000}")
    public void expireStaleReservations() {
        int released = budgetService.expireReservations(Instant.now(), properties.getMaxHold());
        if (released > 0) {
            log.info("budget reservations expired, released={}", released);
        }
    }
}
