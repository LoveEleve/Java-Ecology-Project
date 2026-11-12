package com.loveeleve.ecology.redis.service;

import com.loveeleve.ecology.redis.config.RedisPubSubConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PubSubService {

    private final StringRedisTemplate redisTemplate;

    public PubSubService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long publish(String message) {
        Long receivers = redisTemplate.convertAndSend(RedisPubSubConfig.CHANNEL, message);
        return receivers == null ? 0 : receivers;
    }
}
