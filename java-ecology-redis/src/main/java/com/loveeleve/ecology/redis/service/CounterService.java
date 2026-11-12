package com.loveeleve.ecology.redis.service;

import com.loveeleve.ecology.redis.config.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CounterService {

    private final StringRedisTemplate redisTemplate;

    public CounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long increment(String name) {
        Long value = redisTemplate.opsForValue().increment(RedisKeys.counter(name));
        return value == null ? 0 : value;
    }

    public String get(String name) {
        return redisTemplate.opsForValue().get(RedisKeys.counter(name));
    }
}
