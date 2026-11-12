package com.loveeleve.ecology.redisson.service;

import com.loveeleve.ecology.redisson.config.RedissonKeys;
import org.redisson.api.RBucket;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AsyncService {

    private final RedissonClient redissonClient;

    public AsyncService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public String setAndGet(String name, String value) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(RedissonKeys.async(name));
            RFuture<Void> setFuture = bucket.setAsync(value);
            setFuture.get(3, TimeUnit.SECONDS);
            RFuture<String> getFuture = bucket.getAsync();
            return getFuture.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("async Redis operation interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("async Redis operation failed", ex);
        }
    }
}
