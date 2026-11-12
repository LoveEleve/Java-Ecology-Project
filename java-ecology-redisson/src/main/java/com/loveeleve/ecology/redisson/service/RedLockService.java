package com.loveeleve.ecology.redisson.service;

import com.loveeleve.ecology.redisson.config.RedissonKeys;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedLockService {

    private final RedissonClient redissonClient;

    public RedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean tryLock(String name, long waitMillis, long leaseMillis) {
        if (waitMillis < 0 || leaseMillis <= 0) {
            throw new IllegalArgumentException("waitMillis must be >= 0 and leaseMillis must be positive");
        }
        RLock first = redissonClient.getLock(RedissonKeys.redLock(name) + ":0");
        RLock second = redissonClient.getLock(RedissonKeys.redLock(name) + ":1");
        RedissonRedLock lock = new RedissonRedLock(first, second);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitMillis, leaseMillis, TimeUnit.MILLISECONDS);
            return acquired;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while acquiring RedLock", ex);
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }
}
