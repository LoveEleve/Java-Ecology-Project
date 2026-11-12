package com.loveeleve.ecology.redis.service;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedisTransactionService {

    private final StringRedisTemplate redisTemplate;

    public RedisTransactionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<Object> multiSet(String key1, String value1, String key2, String value2) {
        return redisTemplate.execute(new SessionCallback<>() {
            @Override
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) {
                RedisOperations<String, String> stringOperations = (RedisOperations<String, String>) operations;
                stringOperations.multi();
                stringOperations.opsForValue().set(key1, value1);
                stringOperations.opsForValue().set(key2, value2);
                return stringOperations.exec();
            }
        });
    }
}
