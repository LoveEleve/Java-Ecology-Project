package com.loveeleve.ecology.budget.lease;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 本地号段管理器。
 *
 * <p>核心流程：</p>
 * <ol>
 *     <li>请求优先从本地 lease 扣；</li>
 *     <li>低于低水位时，CAS 单飞触发异步续段；</li>
 *     <li>本地耗尽时按状态快速失败/有限等待；</li>
 *     <li>DB 申请使用 leaseRequestId 幂等，响应丢失用同一个 ID 查询/重试。</li>
 * </ol>
 *
 * <p>生产上：</p>
 * <ul>
 *     <li>本地号段来源是 MySQL/Redis 中央账户；</li>
 *     <li>续段异步线程池要隔离，不能占用业务线程；</li>
 *     <li>活动结束/预算关闭要把状态置 TERMINAL。</li>
 * </ul>
 */
public final class LeaseManager implements AutoCloseable {

    private final InMemoryBudgetStore store;
    private final String instanceId;
    private final ConcurrentHashMap<BudgetAccountKey, LeaseSlot> slots = new ConcurrentHashMap<>();
    private final ExecutorService refillExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "budget-lease-refill");
        thread.setDaemon(true);
        return thread;
    });

    public LeaseManager(InMemoryBudgetStore store, String instanceId) {
        this.store = store;
        this.instanceId = instanceId;
    }

    /**
     * 通过本地号段预占。
     *
     * <p>本地号段不够时不会无限等待：先尝试续段，当前请求快速失败；
     * 下一笔请求在低水位/恢复后继续服务。</p>
     */
    public ReserveResult reserve(ReserveRequest request) {
        BudgetAccountKey key = BudgetAccountKey.sub(
                request.activityId(), request.sourceCandidates().get(0),
                request.templateId(), request.unit());

        LeaseSlot slot = slots.computeIfAbsent(key, LeaseSlot::new);

        if (slot.leaseId() == null) {
            refillNow(slot, request, request.sourceCandidates().get(0));
        }

        if (slot.tryAcquire(request.amount())) {
            try {
                BudgetFlow flow = store.reserveFromLease(request, slot.leaseId());
                maybeRefill(slot, request, request.sourceCandidates().get(0));
                return ReserveResult.existing(flow);
            } catch (RuntimeException ex) {
                slot.release(request.amount());
                throw ex;
            }
        }

        // 本地不够：触发异步续段，当前请求失败，不阻塞业务线程。
        maybeRefill(slot, request, request.sourceCandidates().get(0));
        throw new BudgetException(BudgetException.Code.LEASE_EXHAUSTED,
                "local lease exhausted: " + key);
    }

    /** 确认号段内预占。 */
    public BudgetFlow confirm(String txnId) {
        return store.confirmLeaseFlow(txnId);
    }

    /** 释放号段内预占，并把额度还回本地号段。 */
    public BudgetFlow release(String txnId) {
        BudgetFlow flow = store.releaseLeaseFlow(txnId);
        BudgetAccountKey key = BudgetAccountKey.sub(
                flow.activityId(), flow.sourceId(), flow.templateId(), flow.unit());
        LeaseSlot slot = slots.get(key);
        if (slot != null) {
            slot.release(flow.amount());
        }
        return flow;
    }

    /** 归还整个号段。 */
    public void returnLease(LeaseSlot slot) {
        if (slot.leaseId() == null) {
            return;
        }
        store.returnLease(slot.leaseId());
        slot.state(LeaseState.TERMINAL);
    }

    /**
     * 低水位判断 + CAS 单飞。
     *
     * <p>只有 ACTIVE 才允许触发续段；DRAINING/EXHAUSTED/TERMINAL 不触发。</p>
     */
    private void maybeRefill(LeaseSlot slot, ReserveRequest request, String sourceId) {
        BigDecimal remaining = slot.remaining();
        BigDecimal lowWatermark = slot.chunk().multiply(new BigDecimal("0.2"));
        if (remaining.compareTo(lowWatermark) > 0) {
            return;
        }
        if (slot.state() != LeaseState.ACTIVE) {
            return;
        }
        if (!slot.casState(LeaseState.ACTIVE, LeaseState.REFILLING)) {
            return;
        }
        refillExecutor.submit(() -> doRefill(slot, request, sourceId));
    }

    /** 首次请求懒加载：同步申请，调用方最多等一次 DB。 */
    private void refillNow(LeaseSlot slot, ReserveRequest request, String sourceId) {
        if (!slot.casState(LeaseState.EXHAUSTED, LeaseState.REFILLING)
                && !slot.casState(LeaseState.ACTIVE, LeaseState.REFILLING)) {
            return;
        }
        doRefill(slot, request, sourceId);
    }

    /**
     * 真正申请号段。
     *
     * <p>生产上这里调用 DB/中央服务；leaseRequestId 必须持久化/可查询。
     * 本示例直接调用 InMemoryBudgetStore。</p>
     */
    private void doRefill(LeaseSlot slot, ReserveRequest request, String sourceId) {
        try {
            long desired = slot.sizer().nextChunk(
                    slot.rate().ratePerSecond(),
                    slot.chunk().max(BigDecimal.valueOf(1000)).longValue(),
                    1);
            BigDecimal chunk = BigDecimal.valueOf(desired);
            BudgetLease lease = store.allocateLease(new LeaseRequest(
                    UUID.randomUUID().toString(), instanceId,
                    request.activityId(), sourceId, request.templateId(), request.unit(),
                    chunk, Instant.now().plus(Duration.ofMinutes(2))));
            slot.installLease(lease.leaseId(), lease.chunk());
        } catch (BudgetException ex) {
            if (ex.code() == BudgetException.Code.INSUFFICIENT_BUDGET) {
                slot.state(slot.remaining().signum() > 0
                        ? LeaseState.DRAINING : LeaseState.EXHAUSTED);
                slot.nextProbeAtNanos(System.nanoTime() + Duration.ofSeconds(5).toNanos());
            } else {
                slot.state(LeaseState.BACKOFF);
                slot.nextProbeAtNanos(System.nanoTime() + Duration.ofSeconds(1).toNanos());
            }
        } catch (RuntimeException ex) {
            slot.state(LeaseState.BACKOFF);
            slot.nextProbeAtNanos(System.nanoTime() + Duration.ofSeconds(1).toNanos());
        }
    }

    @Override
    public void close() {
        refillExecutor.shutdownNow();
        try {
            refillExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
