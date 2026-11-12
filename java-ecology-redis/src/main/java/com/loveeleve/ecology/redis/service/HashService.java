package com.loveeleve.ecology.redis.service;

import com.loveeleve.ecology.redis.config.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HashService {

    private final StringRedisTemplate redisTemplate;

    public HashService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void putAll(String name, Map<String, String> values) {
        redisTemplate.opsForHash().putAll(RedisKeys.hash(name), values);
    }

    public Map<Object, Object> entries(String name) {
        return redisTemplate.opsForHash().entries(RedisKeys.hash(name));
    }
}
