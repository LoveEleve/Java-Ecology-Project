package com.loveeleve.ecology.sharding.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderItemRequest(
        @NotNull @Min(1) Long itemId,
        @NotNull @Min(1) Long orderId,
        @NotNull @Min(1) Long userId,
        @NotBlank @Size(max = 64) String productName,
        @Min(1) int quantity
) {
}
