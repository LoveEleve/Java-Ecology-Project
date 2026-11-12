package com.loveeleve.ecology.redisson.service;

import com.loveeleve.ecology.redisson.config.RedissonKeys;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MapCacheService {

    private final RedissonClient redissonClient;

    public MapCacheService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void put(String name, String key, String value, long ttl, TimeUnit unit) {
        if (ttl <= 0) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        RMapCache<String, String> map = redissonClient.getMapCache(RedissonKeys.map(name));
        map.put(key, value, ttl, unit);
    }

    public String get(String name, String key) {
        RMapCache<String, String> map = redissonClient.getMapCache(RedissonKeys.map(name));
        return map.get(key);
    }
}
