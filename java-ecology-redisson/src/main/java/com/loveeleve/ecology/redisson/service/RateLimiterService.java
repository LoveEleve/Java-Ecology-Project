package com.loveeleve.ecology.redisson.service;

import com.loveeleve.ecology.redisson.config.RedissonKeys;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final RedissonClient redissonClient;

    public RateLimiterService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean tryAcquire(String name, int permitsPerInterval, long interval, RateIntervalUnit unit) {
        if (permitsPerInterval <= 0 || interval <= 0) {
            throw new IllegalArgumentException("rate limiter permits and interval must be positive");
        }
        RRateLimiter limiter = redissonClient.getRateLimiter(RedissonKeys.rateLimiter(name));
        limiter.trySetRate(RateType.OVERALL, permitsPerInterval, interval, unit);
        return limiter.tryAcquire();
    }

    public long availablePermits(String name) {
        return redissonClient.getRateLimiter(RedissonKeys.rateLimiter(name)).availablePermits();
    }
}
