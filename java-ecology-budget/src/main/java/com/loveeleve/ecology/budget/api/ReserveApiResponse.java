package com.loveeleve.ecology.budget.api;

import com.loveeleve.ecology.budget.domain.ReserveResult;

/**
 * 预占接口响应。
 */
public record ReserveApiResponse(
        String txnId,
        String sourceId,
        String status,
        String message) {

    public static ReserveApiResponse from(ReserveResult result) {
        return new ReserveApiResponse(
                result.txnId(), result.sourceId(), result.status().name(), result.message());
    }
}
