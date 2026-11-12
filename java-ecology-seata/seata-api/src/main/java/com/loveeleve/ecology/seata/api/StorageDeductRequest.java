package com.loveeleve.ecology.seata.api;

public record StorageDeductRequest(
        String requestId,
        Long productId,
        Integer quantity) {
}
