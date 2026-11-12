package com.loveeleve.ecology.sharding.dto;

import com.loveeleve.ecology.sharding.model.OrderItem;

public record OrderItemResponse(
        long itemId,
        long orderId,
        long userId,
        String productName,
        int quantity
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.itemId(), item.orderId(), item.userId(),
                item.productName(), item.quantity());
    }
}
