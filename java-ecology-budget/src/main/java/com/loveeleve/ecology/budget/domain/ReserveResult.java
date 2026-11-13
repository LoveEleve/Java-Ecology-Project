package com.loveeleve.ecology.budget.domain;

/**
 * 预占结果。
 *
 * <p>重复请求返回同一个 txnId 和状态，而不是重新扣一次。</p>
 */
public record ReserveResult(
        String txnId,
        String sourceId,
        BudgetFlowStatus status,
        String message) {

    public static ReserveResult success(String txnId, String sourceId) {
        return new ReserveResult(txnId, sourceId, BudgetFlowStatus.RESERVED, "reserved");
    }

    public static ReserveResult existing(BudgetFlow flow) {
        return new ReserveResult(flow.txnId(), flow.sourceId(), flow.status(), "reserved or idempotent replay");
    }
}
