package com.loveeleve.ecology.seata.api;

public record OrderResponse(
        Long orderId,
        String status) {
}
