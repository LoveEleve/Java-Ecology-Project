package com.loveeleve.ecology.budget.store;

import com.loveeleve.ecology.budget.domain.BudgetAccount;
import com.loveeleve.ecology.budget.domain.BudgetAccountKey;
import com.loveeleve.ecology.budget.domain.BudgetFlow;
import com.loveeleve.ecology.budget.domain.BudgetFlowStatus;
import com.loveeleve.ecology.budget.domain.BudgetLease;
import com.loveeleve.ecology.budget.domain.LeaseRequest;
import com.loveeleve.ecology.budget.domain.LeaseStatus;
import com.loveeleve.ecology.budget.domain.ReserveRequest;
import com.loveeleve.ecology.budget.exception.BudgetException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存版预算账本，用来模拟 MySQL 事务/Redis 原子脚本。
 *
 * <p>生产落库对应关系：</p>
 * <pre>
 * accounts  -> t_budget_account
 * flows     -> t_budget_flow
 * leases    -> t_budget_lease
 * </pre>
 *
 * <p>所有“多账户同时扣减”的方法都用 synchronized 包住，模拟一个本地事务：
 * 任一账户不足则整体回滚，不留下半扣状态。生产环境应使用 MySQL 本地事务，
 * 或 Redis Lua 保证同一事务边界。</p>
 */
public final class InMemoryBudgetStore {

    private final Map<BudgetAccountKey, BudgetAccount> accounts = new LinkedHashMap<>();
    private final Map<String, BudgetFlow> flowsById = new LinkedHashMap<>();
    private final Map<String, BudgetFlow> flowsByRequestId = new LinkedHashMap<>();
    private final Map<String, BudgetLease> leasesById = new LinkedHashMap<>();
    private final Map<String, BudgetLease> leasesByRequestId = new LinkedHashMap<>();

    private long txnSequence;
    private long leaseSequence;

    /** 创建预算账户；重复创建直接报错，避免覆盖已有余额。 */
    public synchronized void createAccount(BudgetAccountKey key, BigDecimal total) {
        if (accounts.containsKey(key)) {
            throw new BudgetException(BudgetException.Code.INVALID_ARGUMENT,
                    "account already exists: " + key);
        }
        accounts.put(key, new BudgetAccount(key, total));
    }

    /** 追加预算：只增不减。 */
    public synchronized void append(BudgetAccountKey key, BigDecimal delta) {
        account(key).append(delta);
    }

    /**
     * 直接预占：同时扣来源池总额和券模板子预算。
     *
     * <p>用于没有本地号段的简单路径。高并发路径使用
     * {@link #allocateLease(LeaseRequest)} + {@link #reserveFromLease}。</p>
     */
    public synchronized BudgetFlow reserveDirect(ReserveRequest request, String sourceId) {
        BudgetFlow existing = flowsByRequestId.get(request.requestId());
        if (existing != null) {
            assertSameRequest(existing, request);
            return existing;
        }

        BudgetAccountKey totalKey = BudgetAccountKey.total(
                request.activityId(), sourceId, request.unit());
        BudgetAccountKey subKey = BudgetAccountKey.sub(
                request.activityId(), sourceId, request.templateId(), request.unit());

        BudgetAccount total = account(totalKey);
        BudgetAccount sub = totalKey.equals(subKey) ? null : account(subKey);
        BigDecimal amount = request.amount();

        assertAvailable(total, amount, totalKey);
        if (sub != null) {
            assertAvailable(sub, amount, subKey);
        }

        // 两个账户同时预占；第二个失败时回滚第一个，保证不会半扣。
        total.reserve(amount);
        try {
            if (sub != null) {
                sub.reserve(amount);
            }
        } catch (RuntimeException ex) {
            total.release(amount);
            throw ex;
        }

        BudgetFlow flow = new BudgetFlow(
                nextTxnId(), request.requestId(), request.requestHash(),
                request.activityId(), sourceId, request.templateId(),
                request.unit(), amount, null, Instant.now());
        flowsById.put(flow.txnId(), flow);
        flowsByRequestId.put(flow.requestId(), flow);
        return flow;
    }

    /** 确认预占：reserved -> consumed；重复确认幂等成功。 */
    public synchronized BudgetFlow confirm(String txnId) {
        BudgetFlow flow = flow(txnId);
        if (flow.status() == BudgetFlowStatus.CONFIRMED) {
            return flow;
        }
        if (flow.status() == BudgetFlowStatus.RELEASED) {
            throw new BudgetException(BudgetException.Code.FLOW_STATE_CONFLICT,
                    "flow already released: " + txnId);
        }

        account(BudgetAccountKey.total(flow.activityId(), flow.sourceId(), flow.unit()))
                .confirm(flow.amount());
        if (!BudgetAccountKey.TOTAL_TEMPLATE.equals(flow.templateId())) {
            account(BudgetAccountKey.sub(flow.activityId(), flow.sourceId(),
                    flow.templateId(), flow.unit())).confirm(flow.amount());
        }
        flow.confirm(Instant.now());
        return flow;
    }

    /** 释放预占：reserved 退回；重复释放幂等成功。 */
    public synchronized BudgetFlow release(String txnId) {
        BudgetFlow flow = flow(txnId);
        if (flow.status() == BudgetFlowStatus.RELEASED) {
            return flow;
        }
        if (flow.status() == BudgetFlowStatus.CONFIRMED) {
            throw new BudgetException(BudgetException.Code.FLOW_STATE_CONFLICT,
                    "flow already confirmed: " + txnId);
        }
        releaseAccounts(flow);
        flow.release(Instant.now());
        return flow;
    }

    /** 回收超时未确认的预占；生产上用 XXL-Job/对账任务调度。 */
    public synchronized int expireReservations(Instant now, Duration maxHold) {
        int released = 0;
        for (BudgetFlow flow : flowsById.values()) {
            if (!flow.isExpired(now, maxHold)) {
                continue;
            }
            if (flow.leaseId() != null) {
                BudgetLease lease = leasesById.get(flow.leaseId());
                if (lease != null && lease.status() == LeaseStatus.ACTIVE) {
                    releaseLeaseFlow(flow, lease);
                } else {
                    releaseAccounts(flow);
                    flow.release(now);
                }
            } else {
                releaseAccounts(flow);
                flow.release(now);
            }
            released++;
        }
        return released;
    }

    /**
     * 申请一段号段：中央同时预占来源池总额和模板子预算。
     *
     * <p>leaseRequestId 是申请幂等键；响应丢失后必须用同一个 ID 重试。</p>
     */
    public synchronized BudgetLease allocateLease(LeaseRequest request) {
        BudgetLease existing = leasesByRequestId.get(request.leaseRequestId());
        if (existing != null) {
            assertSameLeaseRequest(existing, request);
            return existing;
        }

        BudgetAccount total = account(request.totalKey());
        BudgetAccount sub = request.totalKey().equals(request.subKey())
                ? null : account(request.subKey());
        assertAvailable(total, request.chunk(), request.totalKey());
        if (sub != null) {
            assertAvailable(sub, request.chunk(), request.subKey());
        }

        total.reserve(request.chunk());
        try {
            if (sub != null) {
                sub.reserve(request.chunk());
            }
        } catch (RuntimeException ex) {
            total.release(request.chunk());
            throw ex;
        }

        BudgetLease lease = new BudgetLease(
                nextLeaseId(), request.leaseRequestId(), request.instanceId(),
                request.subKey(), request.chunk(), request.expireAt());
        leasesById.put(lease.leaseId(), lease);
        leasesByRequestId.put(lease.leaseRequestId(), lease);
        return lease;
    }

    /** 号段内本地预占；不再扣中央账户，只消耗 lease。 */
    public synchronized BudgetFlow reserveFromLease(ReserveRequest request, String leaseId) {
        BudgetFlow existing = flowsByRequestId.get(request.requestId());
        if (existing != null) {
            assertSameRequest(existing, request);
            return existing;
        }

        BudgetLease lease = lease(leaseId);
        if (lease.status() != LeaseStatus.ACTIVE) {
            throw new BudgetException(BudgetException.Code.LEASE_STATE_CONFLICT,
                    "lease is not active: " + leaseId);
        }
        lease.use(request.amount());

        BudgetFlow flow = new BudgetFlow(
                nextTxnId(), request.requestId(), request.requestHash(),
                request.activityId(), lease.key().sourceId(), request.templateId(),
                request.unit(), request.amount(), leaseId, Instant.now());
        flowsById.put(flow.txnId(), flow);
        flowsByRequestId.put(flow.requestId(), flow);
        return flow;
    }

    /** 号段内请求确认：账户 reserved -> consumed；lease.used 不变。 */
    public synchronized BudgetFlow confirmLeaseFlow(String txnId) {
        BudgetFlow flow = flow(txnId);
        if (flow.status() == BudgetFlowStatus.CONFIRMED) {
            return flow;
        }
        if (flow.status() == BudgetFlowStatus.RELEASED) {
            throw new BudgetException(BudgetException.Code.FLOW_STATE_CONFLICT,
                    "flow already released: " + txnId);
        }
        account(BudgetAccountKey.total(flow.activityId(), flow.sourceId(), flow.unit()))
                .confirm(flow.amount());
        if (!BudgetAccountKey.TOTAL_TEMPLATE.equals(flow.templateId())) {
            account(BudgetAccountKey.sub(flow.activityId(), flow.sourceId(),
                    flow.templateId(), flow.unit())).confirm(flow.amount());
        }
        flow.confirm(Instant.now());
        return flow;
    }

    /** 号段内请求释放：账户 reserved 退回，lease.used 减回，可被后续请求复用。 */
    public synchronized BudgetFlow releaseLeaseFlow(String txnId) {
        BudgetFlow flow = flow(txnId);
        if (flow.status() == BudgetFlowStatus.RELEASED) {
            return flow;
        }
        if (flow.status() == BudgetFlowStatus.CONFIRMED) {
            throw new BudgetException(BudgetException.Code.FLOW_STATE_CONFLICT,
                    "flow already confirmed: " + txnId);
        }
        BudgetLease lease = lease(flow.leaseId());
        releaseLeaseFlow(flow, lease);
        return flow;
    }

    /** 归还整个号段：未用额度退回中央，已用部分保持。 */
    public synchronized void returnLease(String leaseId) {
        BudgetLease lease = lease(leaseId);
        if (lease.status() != LeaseStatus.ACTIVE) {
            return;
        }
        BigDecimal unused = lease.remaining();
        if (unused.signum() > 0) {
            account(BudgetAccountKey.total(
                    lease.key().activityId(), lease.key().sourceId(), lease.key().unit()))
                    .release(unused);
            if (!BudgetAccountKey.TOTAL_TEMPLATE.equals(lease.key().templateId())) {
                account(lease.key()).release(unused);
            }
        }
        lease.returnLease();
    }

    public synchronized List<BudgetAccount> accounts() {
        return List.copyOf(accounts.values());
    }

    public synchronized Collection<BudgetFlow> flows() {
        return List.copyOf(flowsById.values());
    }

    public synchronized Collection<BudgetLease> leases() {
        return List.copyOf(leasesById.values());
    }

    private void releaseLeaseFlow(BudgetFlow flow, BudgetLease lease) {
        account(BudgetAccountKey.total(flow.activityId(), flow.sourceId(), flow.unit()))
                .release(flow.amount());
        if (!BudgetAccountKey.TOTAL_TEMPLATE.equals(flow.templateId())) {
            account(BudgetAccountKey.sub(flow.activityId(), flow.sourceId(),
                    flow.templateId(), flow.unit())).release(flow.amount());
        }
        lease.releaseUse(flow.amount());
        flow.release(Instant.now());
    }

    private void releaseAccounts(BudgetFlow flow) {
        account(BudgetAccountKey.total(flow.activityId(), flow.sourceId(), flow.unit()))
                .release(flow.amount());
        if (!BudgetAccountKey.TOTAL_TEMPLATE.equals(flow.templateId())) {
            account(BudgetAccountKey.sub(flow.activityId(), flow.sourceId(),
                    flow.templateId(), flow.unit())).release(flow.amount());
        }
    }

    private BudgetAccount account(BudgetAccountKey key) {
        BudgetAccount account = accounts.get(key);
        if (account == null) {
            throw new BudgetException(BudgetException.Code.BUDGET_NOT_FOUND,
                    "budget account not found: " + key);
        }
        return account;
    }

    private BudgetFlow flow(String txnId) {
        BudgetFlow flow = flowsById.get(txnId);
        if (flow == null) {
            throw new BudgetException(BudgetException.Code.FLOW_NOT_FOUND,
                    "budget flow not found: " + txnId);
        }
        return flow;
    }

    private BudgetLease lease(String leaseId) {
        BudgetLease lease = leasesById.get(leaseId);
        if (lease == null) {
            throw new BudgetException(BudgetException.Code.LEASE_NOT_FOUND,
                    "budget lease not found: " + leaseId);
        }
        return lease;
    }

    private void assertAvailable(BudgetAccount account, BigDecimal amount, BudgetAccountKey key) {
        if (account.availableAmount().compareTo(amount) < 0) {
            throw new BudgetException(BudgetException.Code.INSUFFICIENT_BUDGET,
                    "insufficient budget: " + key + ", available=" + account.availableAmount());
        }
    }

    private void assertSameRequest(BudgetFlow existing, ReserveRequest request) {
        if (!existing.requestHash().equals(request.requestHash())) {
            throw new BudgetException(BudgetException.Code.IDEMPOTENCY_CONFLICT,
                    "requestId reused with different params: " + request.requestId());
        }
    }

    private void assertSameLeaseRequest(BudgetLease existing, LeaseRequest request) {
        // 复用号段申请时，参数必须一致；不一致不能返回旧号段。
        if (!existing.key().equals(request.subKey())
                || existing.chunk().compareTo(request.chunk()) != 0) {
            throw new BudgetException(BudgetException.Code.IDEMPOTENCY_CONFLICT,
                    "leaseRequestId reused with different params: " + request.leaseRequestId());
        }
    }

    private String nextTxnId() {
        return "BTX-" + (++txnSequence);
    }

    private String nextLeaseId() {
        return "LEASE-" + (++leaseSequence);
    }
}
