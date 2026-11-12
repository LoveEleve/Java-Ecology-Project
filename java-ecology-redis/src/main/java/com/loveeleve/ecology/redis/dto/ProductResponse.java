package com.loveeleve.ecology.redis.dto;

import com.loveeleve.ecology.redis.model.Product;

public record ProductResponse(
        long id,
        String name,
        long priceCents
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.id(), product.name(), product.priceCents());
    }
}
