package com.loveeleve.ecology.budget.service;

import com.loveeleve.ecology.budget.domain.BudgetAccount;
import com.loveeleve.ecology.budget.domain.BudgetFlow;
import com.loveeleve.ecology.budget.domain.BudgetFlowStatus;
import com.loveeleve.ecology.budget.domain.BudgetLease;
import com.loveeleve.ecology.budget.store.InMemoryBudgetStore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 预算账户、流水、号段三方对账。
 *
 * <p>生产公式：</p>
 * <pre>
 * account.reserved = sum(active lease 未用) + sum(flow RESERVED)
 * account.consumed = sum(flow CONFIRMED)
 * lease.used       = sum(lease 下 RESERVED + CONFIRMED flow)
 * </pre>
 *
 * <p>本示例只做关键不变量和 flow/lease 明显异常检查。
 * 真实实现应跑 SQL 聚合，差异分自动修复/人工工单。</p>
 */
public final class BudgetReconciler {

    private BudgetReconciler() {
    }

    public static List<String> reconcile(InMemoryBudgetStore store) {
        List<String> issues = new ArrayList<>();

        // 1. 账户自身不变量：total >= reserved + consumed。
        for (BudgetAccount account : store.accounts()) {
            try {
                account.assertInvariant();
            } catch (RuntimeException ex) {
                issues.add("account invariant broken: " + account.key() + ", " + ex.getMessage());
            }
        }

        // 2. 流水状态与金额必须合法。
        for (BudgetFlow flow : store.flows()) {
            if (flow.amount() == null || flow.amount().signum() <= 0) {
                issues.add("invalid flow amount: " + flow.txnId());
            }
            if (flow.status() == null) {
                issues.add("missing flow status: " + flow.txnId());
            }
        }

        // 3. 号段 used 不能超过 chunk。
        for (BudgetLease lease : store.leases()) {
            if (lease.used().compareTo(lease.chunk()) > 0) {
                issues.add("lease used exceeds chunk: " + lease.leaseId());
            }
        }

        // 4. 已确认流水的金额必须能在账户里体现；这里只做示例级检查。
        BigDecimal confirmed = store.flows().stream()
                .filter(flow -> flow.status() == BudgetFlowStatus.CONFIRMED)
                .map(BudgetFlow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (confirmed.signum() < 0) {
            issues.add("confirmed amount negative");
        }

        return issues;
    }
}
