package com.loveeleve.ecology.budget.api;

import com.loveeleve.ecology.budget.domain.BudgetAccountKey;
import com.loveeleve.ecology.budget.domain.BudgetUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 追加预算请求。追加只增不减。
 */
public record AppendApiRequest(
        @NotBlank String activityId,
        @NotBlank String sourceId,
        @NotBlank String templateId,
        @NotNull BudgetUnit unit,
        @NotNull @DecimalMin("0.01") BigDecimal delta) {

    public BudgetAccountKey toKey() {
        return new BudgetAccountKey(activityId, sourceId, templateId, unit);
    }
}
