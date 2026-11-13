package com.loveeleve.ecology.budget.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * 一笔预算动作的明细流水。
 *
 * <p>它是一笔预占/确认/释放的事实记录，也是幂等和恢复的核心：</p>
 * <ul>
 *     <li>{@code requestId}：上游传入的业务幂等键；</li>
 *     <li>{@code requestHash}：参数指纹，防止同一个 requestId 被不同参数复用；</li>
 *     <li>{@code txnId}：预算流水 ID，confirm/release 的幂等键；</li>
 *     <li>{@code leaseId}：如果这笔预占来自本地号段，记录对应租约。</li>
 * </ul>
 */
public final class BudgetFlow {

    private final String txnId;
    private final String requestId;
    private final String requestHash;
    private final String activityId;
    private final String sourceId;
    private final String templateId;
    private final BudgetUnit unit;
    private final BigDecimal amount;
    private final String leaseId;
    private final Instant createdAt;

    private BudgetFlowStatus status;
    private Instant updatedAt;

    public BudgetFlow(
            String txnId,
            String requestId,
            String requestHash,
            String activityId,
            String sourceId,
            String templateId,
            BudgetUnit unit,
            BigDecimal amount,
            String leaseId,
            Instant createdAt) {
        this(txnId, requestId, requestHash, activityId, sourceId, templateId,
                unit, amount, leaseId, createdAt, BudgetFlowStatus.RESERVED, createdAt);
    }

    /**
     * 从数据库重建流水时使用，保留真实状态，不能把已确认/已释放的流水
     * 误恢复成 RESERVED。
     */
    public BudgetFlow(
            String txnId,
            String requestId,
            String requestHash,
            String activityId,
            String sourceId,
            String templateId,
            BudgetUnit unit,
            BigDecimal amount,
            String leaseId,
            Instant createdAt,
            BudgetFlowStatus status,
            Instant updatedAt) {
        this.txnId = requireText(txnId, "txnId");
        this.requestId = requireText(requestId, "requestId");
        this.requestHash = requireText(requestHash, "requestHash");
        this.activityId = requireText(activityId, "activityId");
        this.sourceId = requireText(sourceId, "sourceId");
        this.templateId = requireText(templateId, "templateId");
        this.unit = unit;
        this.amount = amount;
        this.leaseId = leaseId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
    }

    public String txnId() {
        return txnId;
    }

    public String requestId() {
        return requestId;
    }

    public String requestHash() {
        return requestHash;
    }

    public String activityId() {
        return activityId;
    }

    public String sourceId() {
        return sourceId;
    }

    public String templateId() {
        return templateId;
    }

    public BudgetUnit unit() {
        return unit;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String leaseId() {
        return leaseId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public BudgetFlowStatus status() {
        return status;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /** 是否已经超时仍未确认/释放。 */
    public boolean isExpired(Instant now, Duration maxHold) {
        return status == BudgetFlowStatus.RESERVED
                && createdAt.plus(maxHold).isBefore(now);
    }

    /** 幂等确认：已确认直接返回；已释放则冲突。 */
    public void confirm(Instant now) {
        if (status == BudgetFlowStatus.CONFIRMED) {
            return;
        }
        if (status == BudgetFlowStatus.RELEASED) {
            throw new IllegalStateException("flow already released: " + txnId);
        }
        status = BudgetFlowStatus.CONFIRMED;
        updatedAt = now;
    }

    /** 幂等释放：已释放直接返回；已确认则冲突。 */
    public void release(Instant now) {
        if (status == BudgetFlowStatus.RELEASED) {
            return;
        }
        if (status == BudgetFlowStatus.CONFIRMED) {
            throw new IllegalStateException("flow already confirmed: " + txnId);
        }
        status = BudgetFlowStatus.RELEASED;
        updatedAt = now;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
