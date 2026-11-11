package com.loveeleve.ecology.web.model;

import java.time.Instant;

public record User(
        Long id,
        String name,
        String email,
        Integer age,
        Instant createdAt
) {
}
