package com.loveeleve.ecology.redisson.controller;

import com.loveeleve.ecology.redisson.common.ApiResponse;
import com.loveeleve.ecology.redisson.service.AsyncService;
import com.loveeleve.ecology.redisson.service.LockService;
import com.loveeleve.ecology.redisson.service.MapCacheService;
import com.loveeleve.ecology.redisson.service.RateLimiterService;
import com.loveeleve.ecology.redisson.service.RedLockService;
import com.loveeleve.ecology.redisson.service.SemaphoreService;
import org.redisson.api.RateIntervalUnit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/redisson")
public class RedissonController {

    private final LockService lockService;
    private final RateLimiterService rateLimiterService;
    private final SemaphoreService semaphoreService;
    private final MapCacheService mapCacheService;
    private final RedLockService redLockService;
    private final AsyncService asyncService;

    public RedissonController(LockService lockService,
                              RateLimiterService rateLimiterService,
                              SemaphoreService semaphoreService,
                              MapCacheService mapCacheService,
                              RedLockService redLockService,
                              AsyncService asyncService) {
        this.lockService = lockService;
        this.rateLimiterService = rateLimiterService;
        this.semaphoreService = semaphoreService;
        this.mapCacheService = mapCacheService;
        this.redLockService = redLockService;
        this.asyncService = asyncService;
    }

    @PostMapping("/lock/{name}/increment")
    public ApiResponse<Long> incrementUnderLock(@PathVariable String name) {
        return ApiResponse.ok(lockService.incrementUnderLock(name));
    }

    @PostMapping("/try-lock/{name}")
    public ApiResponse<Boolean> tryLock(@PathVariable String name,
                                        @RequestParam(defaultValue = "100") long waitMillis,
                                        @RequestParam(defaultValue = "3000") long leaseMillis) {
        return ApiResponse.ok(lockService.tryLock(name, waitMillis, leaseMillis));
    }

    @PostMapping("/fair-lock/{name}")
    public ApiResponse<Boolean> fairLock(@PathVariable String name,
                                         @RequestParam(defaultValue = "100") long waitMillis) {
        return ApiResponse.ok(lockService.fairLock(name, waitMillis));
    }

    @PostMapping("/read-write/{name}")
    public ApiResponse<String> write(@PathVariable String name, @RequestParam String value) {
        lockService.withWriteLock(name, value);
        return ApiResponse.ok(lockService.withReadLock(name));
    }

    @GetMapping("/read-write/{name}")
    public ApiResponse<String> read(@PathVariable String name) {
        return ApiResponse.ok(lockService.withReadLock(name));
    }

    @PostMapping("/red-lock/{name}")
    public ApiResponse<Boolean> redLock(@PathVariable String name,
                                        @RequestParam(defaultValue = "100") long waitMillis,
                                        @RequestParam(defaultValue = "3000") long leaseMillis) {
        return ApiResponse.ok(redLockService.tryLock(name, waitMillis, leaseMillis));
    }

    @PostMapping("/async/{name}")
    public ApiResponse<String> async(@PathVariable String name, @RequestParam String value) {
        return ApiResponse.ok(asyncService.setAndGet(name, value));
    }

    @PostMapping("/rate-limit/{name}")
    public ApiResponse<Boolean> rateLimit(@PathVariable String name,
                                          @RequestParam(defaultValue = "3") int permits,
                                          @RequestParam(defaultValue = "1") long interval,
                                          @RequestParam(defaultValue = "MINUTES") RateIntervalUnit unit) {
        return ApiResponse.ok(rateLimiterService.tryAcquire(name, permits, interval, unit));
    }

    @GetMapping("/rate-limit/{name}/available")
    public ApiResponse<Long> availableRate(@PathVariable String name) {
        return ApiResponse.ok(rateLimiterService.availablePermits(name));
    }

    @PostMapping("/semaphore/{name}/acquire")
    public ApiResponse<Boolean> acquireSemaphore(@PathVariable String name,
                                                 @RequestParam(defaultValue = "2") int permits) {
        return ApiResponse.ok(semaphoreService.tryAcquire(name, permits));
    }

    @PostMapping("/semaphore/{name}/release")
    public ApiResponse<String> releaseSemaphore(@PathVariable String name) {
        semaphoreService.release(name);
        return ApiResponse.ok("released");
    }

    @GetMapping("/semaphore/{name}/available")
    public ApiResponse<Integer> availableSemaphore(@PathVariable String name) {
        return ApiResponse.ok(semaphoreService.availablePermits(name));
    }

    @PostMapping("/map/{name}")
    public ApiResponse<String> putMap(@PathVariable String name,
                                      @RequestParam String key,
                                      @RequestParam String value,
                                      @RequestParam(defaultValue = "60") long ttlSeconds) {
        mapCacheService.put(name, key, value, ttlSeconds, TimeUnit.SECONDS);
        return ApiResponse.ok(mapCacheService.get(name, key));
    }

    @GetMapping("/map/{name}")
    public ApiResponse<String> getMap(@PathVariable String name, @RequestParam String key) {
        return ApiResponse.ok(mapCacheService.get(name, key));
    }
}
