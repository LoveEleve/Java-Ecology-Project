package com.loveeleve.ecology.redisson;

import com.loveeleve.ecology.redisson.service.LockService;
import com.loveeleve.ecology.redisson.service.MapCacheService;
import com.loveeleve.ecology.redisson.service.RateLimiterService;
import com.loveeleve.ecology.redisson.service.SemaphoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RedissonIntegrationTests {

    private static final RedissonTestServer REDIS = RedissonTestServer.start();

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::host);
        registry.add("spring.data.redis.port", REDIS::port);
        registry.add("spring.data.redis.database", () -> 14);
    }

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private LockService lockService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private SemaphoreService semaphoreService;

    @Autowired
    private MapCacheService mapCacheService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void flushRedis() {
        redissonClient.getKeys().flushdb();
    }

    @Test
    void distributedLockSerializesReadModifyWrite() throws Exception {
        String name = "counter-" + System.nanoTime();
        int workers = 10;
        AtomicInteger success = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                lockService.incrementUnderLock(name);
                success.incrementAndGet();
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(success.get()).isEqualTo(workers);
        assertThat(lockService.incrementUnderLock(name)).isEqualTo(workers + 1L);
    }

    @Test
    void tryLockFailsWhileAnotherThreadHoldsTheLock() throws Exception {
        String name = "held-" + System.nanoTime();
        RLock held = redissonClient.getLock("ecology:redisson:try-lock:" + name);
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService holder = Executors.newSingleThreadExecutor();
        try {
            holder.submit(() -> {
                held.lock();
                acquired.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } finally {
                    held.unlock();
                }
                return null;
            });
            assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(lockService.tryLock(name, 50, 1000)).isFalse();
            release.countDown();
        } finally {
            holder.shutdown();
            holder.awaitTermination(5, TimeUnit.SECONDS);
        }
        assertThat(lockService.tryLock(name, 50, 1000)).isTrue();
    }

    @Test
    void fairLockAndReadWriteLockWork() {
        String name = "rw-" + System.nanoTime();
        assertThat(lockService.fairLock(name, 100)).isTrue();
        lockService.withWriteLock(name, "value-1");
        assertThat(lockService.withReadLock(name)).isEqualTo("value-1");
        assertThat(lockService.withReadLock("missing-" + System.nanoTime())).isNull();
    }

    @Test
    void redissonClientKeepsProductionCustomizations() {
        assertThat(redissonClient.getConfig().isSingleConfig()).isTrue();
        assertThat(redissonClient.getConfig().getCodec()).isInstanceOf(JsonJacksonCodec.class);
        assertThat(redissonClient.getConfig().getLockWatchdogTimeout()).isEqualTo(30_000);
        assertThat(redissonClient.getConfig().useSingleServer().getConnectionPoolSize()).isEqualTo(16);
        assertThat(redissonClient.getConfig().useSingleServer().getConnectionMinimumIdleSize()).isEqualTo(2);
    }

    @Test
    void invalidLockAndSemaphoreArgumentsAreRejected() {
        assertThatThrownBy(() -> lockService.tryLock("invalid", -1, 1000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lockService.fairLock("invalid", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> semaphoreService.tryAcquire("invalid", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mapCacheService.put("invalid", "k", "v", 0, TimeUnit.SECONDS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rateLimiterAndSemaphoreEnforceLimits() {
        String rate = "rate-" + System.nanoTime();
        assertThat(rateLimiterService.tryAcquire(rate, 3, 1, RateIntervalUnit.MINUTES)).isTrue();
        assertThat(rateLimiterService.tryAcquire(rate, 3, 1, RateIntervalUnit.MINUTES)).isTrue();
        assertThat(rateLimiterService.tryAcquire(rate, 3, 1, RateIntervalUnit.MINUTES)).isTrue();
        assertThat(rateLimiterService.tryAcquire(rate, 3, 1, RateIntervalUnit.MINUTES)).isFalse();

        String sem = "sem-" + System.nanoTime();
        assertThat(semaphoreService.tryAcquire(sem, 2)).isTrue();
        assertThat(semaphoreService.tryAcquire(sem, 2)).isTrue();
        assertThat(semaphoreService.tryAcquire(sem, 2)).isFalse();
        semaphoreService.release(sem);
        assertThat(semaphoreService.tryAcquire(sem, 2)).isTrue();
    }

    @Test
    void mapCacheStoresValues() {
        String name = "map-" + System.nanoTime();
        mapCacheService.put(name, "key", "value", 60, TimeUnit.SECONDS);
        assertThat(mapCacheService.get(name, "key")).isEqualTo("value");
    }

    @Test
    void httpEndpointsExposeRedissonCapabilities() throws Exception {
        mockMvc.perform(post("/api/redisson/lock/http-lock/increment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(post("/api/redisson/rate-limit/http-rate")
                        .param("permits", "1")
                        .param("interval", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(post("/api/redisson/rate-limit/http-rate")
                        .param("permits", "1")
                        .param("interval", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        mockMvc.perform(post("/api/redisson/semaphore/http-sem/acquire").param("permits", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(post("/api/redisson/map/http-map")
                        .param("key", "k")
                        .param("value", "v"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("v"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
