package com.loveeleve.ecology.redisson.service;

import com.loveeleve.ecology.redisson.config.RedissonKeys;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LockService {

    private final RedissonClient redissonClient;

    public LockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public long incrementUnderLock(String name) {
        RLock lock = redissonClient.getLock(RedissonKeys.lock(name));
        lock.lock();
        try {
            RMap<String, Integer> state = redissonClient.getMap(RedissonKeys.lockState(name));
            int current = state.getOrDefault("value", 0);
            state.put("value", current + 1);
            return current + 1L;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public boolean tryLock(String name, long waitMillis, long leaseMillis) {
        if (waitMillis < 0 || leaseMillis <= 0) {
            throw new IllegalArgumentException("waitMillis must be >= 0 and leaseMillis must be positive");
        }
        RLock lock = redissonClient.getLock(RedissonKeys.tryLock(name));
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitMillis, leaseMillis, TimeUnit.MILLISECONDS);
            return acquired;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while acquiring lock", ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void lockWithWatchdog(String name, Runnable action) {
        RLock lock = redissonClient.getLock(RedissonKeys.lock(name));
        lock.lock();
        try {
            action.run();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public boolean fairLock(String name, long waitMillis) {
        if (waitMillis < 0) {
            throw new IllegalArgumentException("waitMillis must be >= 0");
        }
        RLock lock = redissonClient.getFairLock(RedissonKeys.fairLock(name));
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
            return acquired;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while acquiring fair lock", ex);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public String withReadLock(String name) {
        var readWriteLock = redissonClient.getReadWriteLock(RedissonKeys.readWriteLock(name));
        var lock = readWriteLock.readLock();
        lock.lock();
        try {
            Object value = redissonClient.getBucket(RedissonKeys.lockState(name)).get();
            return value == null ? null : value.toString();
        } finally {
            lock.unlock();
        }
    }

    public void withWriteLock(String name, String value) {
        var readWriteLock = redissonClient.getReadWriteLock(RedissonKeys.readWriteLock(name));
        var lock = readWriteLock.writeLock();
        lock.lock();
        try {
            redissonClient.getBucket(RedissonKeys.lockState(name)).set(value);
        } finally {
            lock.unlock();
        }
    }
}
