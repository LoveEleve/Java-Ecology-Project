package com.loveeleve.ecology.budget.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 中央预算号段租约。
 *
 * <p>一次租约代表中央已经预占的一段额度，发给某个实例在本地消费。
 * 本地请求不直接改中央账户，只消耗 lease；confirm/release 再由流水结算。</p>
 *
 * <p>字段含义：</p>
 * <ul>
 *     <li>{@code chunk}：本段总额度；</li>
 *     <li>{@code used}：本段已经预占/确认的额度；</li>
 *     <li>{@code remaining = chunk - used}：本段还没用的额度。</li>
 * </ul>
 */
public final class BudgetLease {

    private final String leaseId;
    private final String leaseRequestId;
    private final String instanceId;
    private final BudgetAccountKey key;
    private final BigDecimal chunk;
    private final Instant expireAt;

    private BigDecimal used;
    private LeaseStatus status;

    public BudgetLease(
            String leaseId,
            String leaseRequestId,
            String instanceId,
            BudgetAccountKey key,
            BigDecimal chunk,
            Instant expireAt) {
        this(leaseId, leaseRequestId, instanceId, key, chunk, BigDecimal.ZERO,
                LeaseStatus.ACTIVE, expireAt);
    }

    /**
     * 从数据库重建租约时使用，保留 used/status，不能把已归还/已过期
     * 的租约误恢复成 ACTIVE。
     */
    public BudgetLease(
            String leaseId,
            String leaseRequestId,
            String instanceId,
            BudgetAccountKey key,
            BigDecimal chunk,
            BigDecimal used,
            LeaseStatus status,
            Instant expireAt) {
        this.leaseId = requireText(leaseId, "leaseId");
        this.leaseRequestId = requireText(leaseRequestId, "leaseRequestId");
        this.instanceId = requireText(instanceId, "instanceId");
        this.key = key;
        this.chunk = chunk;
        this.used = used;
        this.status = status;
        this.expireAt = expireAt;
    }

    public String leaseId() {
        return leaseId;
    }

    public String leaseRequestId() {
        return leaseRequestId;
    }

    public String instanceId() {
        return instanceId;
    }

    public BudgetAccountKey key() {
        return key;
    }

    public BigDecimal chunk() {
        return chunk;
    }

    public BigDecimal used() {
        return used;
    }

    public BigDecimal remaining() {
        return chunk.subtract(used);
    }

    public Instant expireAt() {
        return expireAt;
    }

    public LeaseStatus status() {
        return status;
    }

    /** 本地请求预占，号段 used += amount。 */
    public void use(BigDecimal amount) {
        requireActive();
        if (remaining().compareTo(amount) < 0) {
            throw new IllegalStateException("lease remaining insufficient: " + leaseId);
        }
        used = used.add(amount);
    }

    /** 本地释放，号段 used -= amount，让额度可以继续被其他请求使用。 */
    public void releaseUse(BigDecimal amount) {
        requireActive();
        if (used.compareTo(amount) < 0) {
            throw new IllegalStateException("lease release exceeds used: " + leaseId);
        }
        used = used.subtract(amount);
    }

    /** 归还整个号段；调用方先计算未用额度并退回账户。 */
    public void returnLease() {
        requireActive();
        status = LeaseStatus.RETURNED;
    }

    /** 过期回收。 */
    public void expire() {
        if (status == LeaseStatus.ACTIVE) {
            status = LeaseStatus.EXPIRED;
        }
    }

    private void requireActive() {
        if (status != LeaseStatus.ACTIVE) {
            throw new IllegalStateException("lease is not active: " + leaseId);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
