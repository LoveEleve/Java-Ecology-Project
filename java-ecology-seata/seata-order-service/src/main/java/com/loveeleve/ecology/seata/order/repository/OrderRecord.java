package com.loveeleve.ecology.seata.order.repository;

import java.math.BigDecimal;

public record OrderRecord(
        long orderId,
        String status,
        long userId,
        long productId,
        BigDecimal amount,
        int quantity) {
}
