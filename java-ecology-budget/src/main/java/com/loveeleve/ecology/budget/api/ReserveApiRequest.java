package com.loveeleve.ecology.budget.api;

import com.loveeleve.ecology.budget.domain.BudgetUnit;
import com.loveeleve.ecology.budget.domain.ReserveRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预占接口请求。
 *
 * <p>requestId 必须由上游传入；预算服务不假设网关/编排已经去重。</p>
 */
public record ReserveApiRequest(
        @NotBlank String requestId,
        @NotBlank String uin,
        @NotBlank String activityId,
        @NotBlank String templateId,
        @NotNull BudgetUnit unit,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotEmpty List<@NotBlank String> sources) {

    public ReserveRequest toDomain() {
        return new ReserveRequest(
                requestId, uin, activityId, templateId, unit, amount, sources);
    }
}
