package com.loveeleve.ecology.budget.domain;

import java.math.BigDecimal;

/**
 * 预算账户聚合。
 *
 * <p>核心不变量：</p>
 * <pre>
 * totalAmount >= reservedAmount + consumedAmount
 * </pre>
 *
 * <p>账户只保存汇总值，明细在 {@link BudgetFlow}，额度分配在
 * {@code BudgetLease}。对账时账户可以由流水重建。</p>
 */
public final class BudgetAccount {

    private final BudgetAccountKey key;
    private BigDecimal totalAmount;
    private BigDecimal reservedAmount;
    private BigDecimal consumedAmount;
    private long version;

    public BudgetAccount(BudgetAccountKey key, BigDecimal totalAmount) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        requireNonNegative(totalAmount, "totalAmount");
        this.key = key;
        this.totalAmount = totalAmount;
        this.reservedAmount = BigDecimal.ZERO;
        this.consumedAmount = BigDecimal.ZERO;
        this.version = 0;
    }

    public BudgetAccountKey key() {
        return key;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public BigDecimal reservedAmount() {
        return reservedAmount;
    }

    public BigDecimal consumedAmount() {
        return consumedAmount;
    }

    public long version() {
        return version;
    }

    /** 可用额度 = 总额 - 已预占 - 已确认消耗。 */
    public BigDecimal availableAmount() {
        return totalAmount.subtract(reservedAmount).subtract(consumedAmount);
    }

    /** 预占：reserved += amount。 */
    public void reserve(BigDecimal amount) {
        requirePositive(amount, "amount");
        if (availableAmount().compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient budget for " + key);
        }
        reservedAmount = reservedAmount.add(amount);
        version++;
        assertInvariant();
    }

    /** 释放：reserved -= amount。 */
    public void release(BigDecimal amount) {
        requirePositive(amount, "amount");
        if (reservedAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("release exceeds reserved for " + key);
        }
        reservedAmount = reservedAmount.subtract(amount);
        version++;
        assertInvariant();
    }

    /** 确认：reserved -= amount，consumed += amount。 */
    public void confirm(BigDecimal amount) {
        requirePositive(amount, "amount");
        if (reservedAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("confirm exceeds reserved for " + key);
        }
        reservedAmount = reservedAmount.subtract(amount);
        consumedAmount = consumedAmount.add(amount);
        version++;
        assertInvariant();
    }

    /** 追加预算：只增不减。 */
    public void append(BigDecimal delta) {
        requirePositive(delta, "delta");
        totalAmount = totalAmount.add(delta);
        version++;
        assertInvariant();
    }

    /** 检查账户不变量。 */
    public void assertInvariant() {
        if (totalAmount.compareTo(reservedAmount.add(consumedAmount)) < 0) {
            throw new IllegalStateException("budget invariant broken for " + key);
        }
    }

    private static void requireNonNegative(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }

    private static void requirePositive(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(name + " must be > 0");
        }
    }
}
