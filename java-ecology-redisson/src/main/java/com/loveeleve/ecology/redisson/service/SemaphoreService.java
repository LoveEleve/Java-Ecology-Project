package com.loveeleve.ecology.redisson.service;

import com.loveeleve.ecology.redisson.config.RedissonKeys;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class SemaphoreService {

    private final RedissonClient redissonClient;

    public SemaphoreService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean tryAcquire(String name, int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be positive");
        }
        RSemaphore semaphore = redissonClient.getSemaphore(RedissonKeys.semaphore(name));
        semaphore.trySetPermits(permits);
        return semaphore.tryAcquire();
    }

    public int availablePermits(String name) {
        return redissonClient.getSemaphore(RedissonKeys.semaphore(name)).availablePermits();
    }

    public void release(String name) {
        redissonClient.getSemaphore(RedissonKeys.semaphore(name)).release();
    }
}
