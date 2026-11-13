package com.loveeleve.ecology.budget.service;

import com.loveeleve.ecology.budget.domain.BudgetAccountKey;
import com.loveeleve.ecology.budget.domain.BudgetFlow;
import com.loveeleve.ecology.budget.domain.BudgetLease;
import com.loveeleve.ecology.budget.domain.LeaseRequest;
import com.loveeleve.ecology.budget.domain.ReserveRequest;
import com.loveeleve.ecology.budget.domain.ReserveResult;
import com.loveeleve.ecology.budget.exception.BudgetException;
import com.loveeleve.ecology.budget.store.InMemoryBudgetStore;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 预算预扣服务门面。
 *
 * <p>本类只处理预算：预占、确认、释放、追加、超时回收、号段申请。
 * 它不感知券模板内容、券码、核销，也不决定什么时候 confirm/release；
 * 那些是权益编排的职责。</p>
 *
 * <p>失败语义：</p>
 * <ul>
 *     <li>预算不足：换下一个来源；全部不足则返回失败；</li>
 *     <li>参数冲突：同一个 requestId 参数不同，直接冲突；</li>
 *     <li>重复确认/释放：按流水状态幂等返回或冲突。</li>
 * </ul>
 */
public final class BudgetPreDeductService {

    private final InMemoryBudgetStore store;
    private final BudgetSourcePolicy sourcePolicy;

    public BudgetPreDeductService(InMemoryBudgetStore store, BudgetSourcePolicy sourcePolicy) {
        this.store = store;
        this.sourcePolicy = sourcePolicy;
    }

    /**
     * 直接预占：按来源优先级逐个尝试。
     *
     * <p>首选来源不足时，继续尝试下一个来源；这是多资金来源的 fallback。
     * 如果所有来源都不足，抛出最后一个 INSUFFICIENT_BUDGET。</p>
     */
    public ReserveResult reserve(ReserveRequest request) {
        BudgetException lastInsufficient = null;
        for (String sourceId : sourcePolicy.candidates(request)) {
            try {
                BudgetFlow flow = store.reserveDirect(request, sourceId);
                return ReserveResult.existing(flow);
            } catch (BudgetException ex) {
                if (ex.code() == BudgetException.Code.INSUFFICIENT_BUDGET) {
                    lastInsufficient = ex;
                    continue;
                }
                throw ex;
            }
        }
        if (lastInsufficient != null) {
            throw lastInsufficient;
        }
        throw new BudgetException(BudgetException.Code.INSUFFICIENT_BUDGET,
                "no source available for request " + request.requestId());
    }

    /** 确认预占；重复调用幂等。 */
    public BudgetFlow confirm(String txnId) {
        return store.confirm(txnId);
    }

    /** 释放预占；重复调用幂等。 */
    public BudgetFlow release(String txnId) {
        return store.release(txnId);
    }

    /** 追加预算；只增不减。 */
    public void append(BudgetAccountKey key, BigDecimal delta) {
        store.append(key, delta);
    }

    /** 回收超时预占；生产上用 XXL-Job/对账任务调度。 */
    public int expire(Instant now, Duration maxHold) {
        return store.expireReservations(now, maxHold);
    }

    /** 申请一个本地号段。 */
    public BudgetLease allocateLease(LeaseRequest request) {
        return store.allocateLease(request);
    }

    /** 从本地号段预占，不再访问中央账户。 */
    public BudgetFlow reserveFromLease(ReserveRequest request, String leaseId) {
        return store.reserveFromLease(request, leaseId);
    }

    /** 号段内确认。 */
    public BudgetFlow confirmLeaseFlow(String txnId) {
        return store.confirmLeaseFlow(txnId);
    }

    /** 号段内释放。 */
    public BudgetFlow releaseLeaseFlow(String txnId) {
        return store.releaseLeaseFlow(txnId);
    }

    /** 归还号段未用额度。 */
    public void returnLease(String leaseId) {
        store.returnLease(leaseId);
    }

    /** 列出账户，供对账/演示使用。 */
    public List<com.loveeleve.ecology.budget.domain.BudgetAccount> accounts() {
        return new ArrayList<>(store.accounts());
    }
}
