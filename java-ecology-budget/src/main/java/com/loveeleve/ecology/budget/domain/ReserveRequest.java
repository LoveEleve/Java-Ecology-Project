package com.loveeleve.ecology.budget.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 预占请求。
 *
 * <p>上游必须传 requestId；预算服务不能假设网关或编排系统已经去重。
 * sourceCandidates 表示本次允许尝试的资金来源，顺序即优先级。</p>
 */
public record ReserveRequest(
        String requestId,
        String uin,
        String activityId,
        String templateId,
        BudgetUnit unit,
        BigDecimal amount,
        List<String> sourceCandidates) {

    public ReserveRequest {
        requireText(requestId, "requestId");
        requireText(uin, "uin");
        requireText(activityId, "activityId");
        requireText(templateId, "templateId");
        Objects.requireNonNull(unit, "unit");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        if (sourceCandidates == null || sourceCandidates.isEmpty()) {
            throw new IllegalArgumentException("sourceCandidates must not be empty");
        }
        sourceCandidates = List.copyOf(sourceCandidates);
    }

    /**
     * 参数指纹：同一个 requestId 但参数不同，必须返回幂等冲突。
     *
     * <p>注意：source 不放进 hash。来源是扣减时选择的，不是请求语义的一部分；
     * 但 activityId/templateId/amount/unit 参与，防止调用方复用 requestId。</p>
     */
    public String requestHash() {
        return activityId + "|" + templateId + "|" + unit + "|" + amount.stripTrailingZeros().toPlainString();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
