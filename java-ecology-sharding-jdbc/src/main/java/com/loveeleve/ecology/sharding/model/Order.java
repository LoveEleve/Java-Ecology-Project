package com.loveeleve.ecology.sharding.model;

public record Order(
        long orderId,
        long userId,
        long amount,
        String status
) {
}
