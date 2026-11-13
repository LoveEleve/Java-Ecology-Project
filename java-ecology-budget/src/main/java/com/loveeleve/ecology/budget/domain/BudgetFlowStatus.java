package com.loveeleve.ecology.budget.domain;

/**
 * 预算流水状态。
 *
 * <pre>
 * RESERVED -> CONFIRMED
 * RESERVED -> RELEASED
 * </pre>
 *
 * <p>只有 RESERVED 可以变成 CONFIRMED/RELEASED；所有更新都带旧状态条件，
 * 防止重复 confirm/release。</p>
 */
public enum BudgetFlowStatus {
    RESERVED,
    CONFIRMED,
    RELEASED
}
