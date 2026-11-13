package com.loveeleve.ecology.budget.api;

import com.loveeleve.ecology.budget.domain.BudgetUnit;
import com.loveeleve.ecology.budget.domain.LeaseRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * 号段申请请求。
 */
public record LeaseApiRequest(
        @NotBlank String leaseRequestId,
        @NotBlank String instanceId,
        @NotBlank String activityId,
        @NotBlank String sourceId,
        @NotBlank String templateId,
        @NotNull BudgetUnit unit,
        @NotNull @DecimalMin("0.01") BigDecimal chunk) {

    public LeaseRequest toDomain(Duration ttl) {
        return new LeaseRequest(
                leaseRequestId, instanceId, activityId, sourceId, templateId,
                unit, chunk, Instant.now().plus(ttl));
    }
}
