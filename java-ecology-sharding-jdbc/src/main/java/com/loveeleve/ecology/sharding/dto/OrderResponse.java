package com.loveeleve.ecology.sharding.dto;

import com.loveeleve.ecology.sharding.model.Order;

public record OrderResponse(
        long orderId,
        long userId,
        long amount,
        String status
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.orderId(), order.userId(), order.amount(), order.status());
    }
}
