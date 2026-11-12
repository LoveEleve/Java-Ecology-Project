package com.loveeleve.ecology.redis.service;

import com.loveeleve.ecology.redis.dto.CreateProductRequest;
import com.loveeleve.ecology.redis.exception.ProductNotFoundException;
import com.loveeleve.ecology.redis.model.Product;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductCacheService {

    private final Map<Long, Product> database = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    public ProductCacheService() {
        database.put(1L, new Product(1L, "keyboard", 19900));
        database.put(2L, new Product(2L, "mouse", 9900));
        sequence.set(3);
    }

    @Cacheable(cacheNames = "product", key = "#id")
    public Product get(long id) {
        Product product = database.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    @CachePut(cacheNames = "product", key = "#result.id()")
    public Product create(CreateProductRequest request) {
        long id = sequence.getAndIncrement();
        Product product = new Product(id, request.name(), request.priceCents());
        database.put(id, product);
        return product;
    }

    @CacheEvict(cacheNames = "product", key = "#id")
    public void delete(long id) {
        if (database.remove(id) == null) {
            throw new ProductNotFoundException(id);
        }
    }
}
