package com.loveeleve.ecology.seata.api;

import java.math.BigDecimal;

public record CreateOrderRequest(
        String requestId,
        Long userId,
        Long productId,
        BigDecimal amount,
        Integer quantity) {
}
