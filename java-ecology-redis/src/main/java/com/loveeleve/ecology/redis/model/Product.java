package com.loveeleve.ecology.redis.model;

public record Product(
        long id,
        String name,
        long priceCents
) {
}
