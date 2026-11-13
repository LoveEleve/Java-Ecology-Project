package com.loveeleve.ecology.budget.demo;

import com.loveeleve.ecology.budget.domain.BudgetAccountKey;
import com.loveeleve.ecology.budget.domain.BudgetFlow;
import com.loveeleve.ecology.budget.domain.BudgetLease;
import com.loveeleve.ecology.budget.domain.BudgetUnit;
import com.loveeleve.ecology.budget.domain.LeaseRequest;
import com.loveeleve.ecology.budget.domain.ReserveRequest;
import com.loveeleve.ecology.budget.domain.ReserveResult;
import com.loveeleve.ecology.budget.service.BudgetPreDeductService;
import com.loveeleve.ecology.budget.service.BudgetReconciler;
import com.loveeleve.ecology.budget.service.PriorityBudgetSourcePolicy;
import com.loveeleve.ecology.budget.store.InMemoryBudgetStore;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 预扣服务演示：不依赖 Spring/MySQL/Redis，直接跑通核心业务链路。
 *
 * <p>演示内容：</p>
 * <ol>
 *     <li>创建总预算 + 子预算；</li>
 *     <li>直接预占、确认、重复确认；</li>
 *     <li>追加预算；</li>
 *     <li>申请号段、从号段预占、确认、归还号段；</li>
 *     <li>超时回收；</li>
 *     <li>三方对账。</li>
 * </ol>
 */
public final class BudgetDemo {

    public static void main(String[] args) {
        InMemoryBudgetStore store = new InMemoryBudgetStore();
        BudgetPreDeductService budget = new BudgetPreDeductService(
                store, new PriorityBudgetSourcePolicy());

        String activity = "A1";
        String market = "market";
        String product = "product";
        String template = "T1";

        // 1. 财务建预算：市场部 1000，产品部 5000；T1 子预算 300。
        store.createAccount(BudgetAccountKey.total(activity, market, BudgetUnit.AMOUNT),
                new BigDecimal("1000"));
        store.createAccount(BudgetAccountKey.sub(activity, market, template, BudgetUnit.AMOUNT),
                new BigDecimal("300"));
        store.createAccount(BudgetAccountKey.total(activity, product, BudgetUnit.AMOUNT),
                new BigDecimal("5000"));
        store.createAccount(BudgetAccountKey.sub(activity, product, template, BudgetUnit.AMOUNT),
                new BigDecimal("2000"));

        // 2. 直接预占：按市场部优先，市场部不足会 fallback 到产品部。
        ReserveRequest r1 = new ReserveRequest(
                "R1", "U1", activity, template, BudgetUnit.AMOUNT,
                new BigDecimal("20"), List.of(market, product));
        ReserveResult result1 = budget.reserve(r1);
        System.out.println("reserve R1 -> " + result1);

        // 重复 requestId：应返回同一个 txnId，不重复扣。
        ReserveResult replay = budget.reserve(r1);
        System.out.println("replay R1 -> " + replay);

        // 3. 确认：reserved -> consumed，重复确认幂等。
        BudgetFlow confirmed = budget.confirm(result1.txnId());
        System.out.println("confirm R1 -> " + confirmed.status());
        System.out.println("confirm R1 again -> " + budget.confirm(result1.txnId()).status());

        // 4. 追加预算：只增不减。
        budget.append(BudgetAccountKey.total(activity, market, BudgetUnit.AMOUNT),
                new BigDecimal("100"));
        budget.append(BudgetAccountKey.sub(activity, market, template, BudgetUnit.AMOUNT),
                new BigDecimal("100"));
        System.out.println("append market total +100, T1 sub +100");

        // 5. 号段：中央申请 200，再从本地号段预占 30。
        BudgetLease lease = budget.allocateLease(new LeaseRequest(
                "LR1", "instance-A", activity, market, template,
                BudgetUnit.AMOUNT, new BigDecimal("200"),
                Instant.now().plus(Duration.ofMinutes(2))));
        System.out.println("lease -> " + lease.leaseId() + ", chunk=" + lease.chunk());

        ReserveRequest r2 = new ReserveRequest(
                "R2", "U2", activity, template, BudgetUnit.AMOUNT,
                new BigDecimal("30"), List.of(market, product));
        BudgetFlow leaseFlow = budget.reserveFromLease(r2, lease.leaseId());
        System.out.println("reserve from lease R2 -> " + leaseFlow.txnId()
                + ", lease remaining=" + lease.remaining());

        budget.confirmLeaseFlow(leaseFlow.txnId());
        budget.returnLease(lease.leaseId());
        System.out.println("confirm R2 and return lease -> remaining=" + lease.remaining());

        // 6. 超时回收：R3 预占后不确认，模拟对账任务回收。
        ReserveRequest r3 = new ReserveRequest(
                "R3", "U3", activity, template, BudgetUnit.AMOUNT,
                new BigDecimal("20"), List.of(market, product));
        budget.reserve(r3);
        int expired = budget.expire(Instant.now().plus(Duration.ofHours(2)), Duration.ofMinutes(30));
        System.out.println("expired flows -> " + expired);

        // 7. 三方对账：示例只检查关键不变量。
        System.out.println("reconcile -> " + BudgetReconciler.reconcile(store));
        System.out.println("accounts:");
        budget.accounts().forEach(account -> System.out.println(
                "  " + account.key() + " total=" + account.totalAmount()
                        + " reserved=" + account.reservedAmount()
                        + " consumed=" + account.consumedAmount()
                        + " available=" + account.availableAmount()));
    }
}
