package com.loveeleve.ecology.druid.model;

import java.time.LocalDateTime;

public record PoolUser(
        Long id,
        String username,
        String email,
        Integer age,
        LocalDateTime createdAt
) {
}
