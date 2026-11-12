package com.loveeleve.ecology.redis.service;

import com.loveeleve.ecology.redis.config.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockService {

    private static final DefaultRedisScript<Long> DEDUCT_SCRIPT = new DefaultRedisScript<>("""
            local stock = tonumber(redis.call('get', KEYS[1]) or '-1')
            if stock < 0 then
                return -1
            end
            if stock == 0 then
                return -2
            end
            return redis.call('decr', KEYS[1])
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public StockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void init(String sku, long stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
        redisTemplate.opsForValue().set(RedisKeys.stock(sku), String.valueOf(stock));
    }

    public long deduct(String sku) {
        Long remaining = redisTemplate.execute(DEDUCT_SCRIPT, List.of(RedisKeys.stock(sku)));
        if (remaining == null) {
            throw new IllegalStateException("stock script returned null");
        }
        if (remaining == -1L) {
            throw new IllegalStateException("stock is not initialized");
        }
        if (remaining == -2L) {
            throw new IllegalStateException("stock is sold out");
        }
        return remaining;
    }
}
