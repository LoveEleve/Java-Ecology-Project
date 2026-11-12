package com.loveeleve.ecology.seata.api;

import java.math.BigDecimal;

public record AccountDebitRequest(
        String requestId,
        Long userId,
        BigDecimal amount) {
}
