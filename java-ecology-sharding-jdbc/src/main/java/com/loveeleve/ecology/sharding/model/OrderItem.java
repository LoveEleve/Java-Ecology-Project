package com.loveeleve.ecology.sharding.model;

public record OrderItem(
        long itemId,
        long orderId,
        long userId,
        String productName,
        int quantity
) {
}
