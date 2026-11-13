package com.loveeleve.ecology.budget.api;

import java.math.BigDecimal;

/**
 * 预算账户查询视图。
 */
public record BudgetAccountView(
        String activityId,
        String sourceId,
        String templateId,
        String unit,
        BigDecimal total,
        BigDecimal reserved,
        BigDecimal consumed,
        BigDecimal available,
        long version) {
}
