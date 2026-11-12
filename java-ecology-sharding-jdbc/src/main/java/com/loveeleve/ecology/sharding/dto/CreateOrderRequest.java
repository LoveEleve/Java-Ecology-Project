package com.loveeleve.ecology.sharding.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull @Min(1) Long orderId,
        @NotNull @Min(1) Long userId,
        @NotNull @Min(1) Long amount,
        @NotBlank @Size(max = 32) String status
) {
}
