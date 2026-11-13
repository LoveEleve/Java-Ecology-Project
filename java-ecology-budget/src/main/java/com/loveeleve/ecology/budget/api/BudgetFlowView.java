package com.loveeleve.ecology.budget.api;

import com.loveeleve.ecology.budget.domain.BudgetFlow;

/**
 * 流水查询视图。
 */
public record BudgetFlowView(
        String txnId,
        String requestId,
        String activityId,
        String sourceId,
        String templateId,
        String unit,
        String amount,
        String status,
        String leaseId) {

    public static BudgetFlowView from(BudgetFlow flow) {
        return new BudgetFlowView(
                flow.txnId(), flow.requestId(), flow.activityId(), flow.sourceId(),
                flow.templateId(), flow.unit().name(), flow.amount().toPlainString(),
                flow.status().name(), flow.leaseId());
    }
}
