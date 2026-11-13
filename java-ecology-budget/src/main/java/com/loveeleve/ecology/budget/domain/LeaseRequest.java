package com.loveeleve.ecology.budget.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 中央号段申请请求。
 *
 * <p>leaseRequestId 是申请幂等键：MySQL 申请成功但响应丢失时，用同一个
 * leaseRequestId 重试，不能生成新 ID 重复领一段。</p>
 */
public record LeaseRequest(
        String leaseRequestId,
        String instanceId,
        String activityId,
        String sourceId,
        String templateId,
        BudgetUnit unit,
        BigDecimal chunk,
        Instant expireAt) {

    public LeaseRequest {
        requireText(leaseRequestId, "leaseRequestId");
        requireText(instanceId, "instanceId");
        requireText(activityId, "activityId");
        requireText(sourceId, "sourceId");
        requireText(templateId, "templateId");
        Objects.requireNonNull(unit, "unit");
        if (chunk == null || chunk.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("chunk must be > 0");
        }
        Objects.requireNonNull(expireAt, "expireAt");
    }

    public BudgetAccountKey totalKey() {
        return BudgetAccountKey.total(activityId, sourceId, unit);
    }

    public BudgetAccountKey subKey() {
        return BudgetAccountKey.sub(activityId, sourceId, templateId, unit);
    }

    /** 申请参数指纹，防止同一个 leaseRequestId 被不同参数复用。 */
    public String requestHash() {
        return activityId + "|" + sourceId + "|" + templateId + "|" + unit + "|"
                + chunk.stripTrailingZeros().toPlainString();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
