package com.loveeleve.ecology.redis;

import com.loveeleve.ecology.redis.dto.CreateProductRequest;
import com.loveeleve.ecology.redis.service.CounterService;
import com.loveeleve.ecology.redis.service.HashService;
import com.loveeleve.ecology.redis.service.LeaderboardService;
import com.loveeleve.ecology.redis.service.PipelineService;
import com.loveeleve.ecology.redis.service.ProductCacheService;
import com.loveeleve.ecology.redis.service.RateLimiterService;
import com.loveeleve.ecology.redis.service.RedisTransactionService;
import com.loveeleve.ecology.redis.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
class RedisIntegrationTests {

    private static final RedisTestServer REDIS = RedisTestServer.start();

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::host);
        registry.add("spring.data.redis.port", REDIS::port);
        registry.add("spring.data.redis.database", () -> 15);
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private HashService hashService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private StockService stockService;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private RedisTransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void flushRedis() {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void cacheStoresJsonAndEvictsOnDelete() {
        String name = "cache-" + System.nanoTime();
        var product = productCacheService.create(new CreateProductRequest(name, 12300));
        String key = "ecology:redis:cache:product::" + product.id();

        assertThat(productCacheService.get(product.id()).name()).isEqualTo(name);
        assertThat(redisTemplate.hasKey(key)).isTrue();
        assertThat(redisTemplate.getExpire(key)).isPositive();

        productCacheService.delete(product.id());
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    void dataStructuresAndPipelineWork() {
        assertThat(counterService.increment("visits")).isEqualTo(1);
        assertThat(counterService.increment("visits")).isEqualTo(2);
        assertThat(counterService.get("visits")).isEqualTo("2");

        hashService.putAll("user:1", Map.of("name", "alice", "zone", "cn"));
        assertThat(hashService.entries("user:1")).containsEntry("name", "alice");

        leaderboardService.add("score", "alice", 100);
        leaderboardService.add("score", "bob", 200);
        assertThat(leaderboardService.top("score", 2))
                .extracting(LeaderboardService.RankEntry::member)
                .containsExactly("bob", "alice");

        pipelineService.write("batch", Map.of("a", "1", "b", "2"), Duration.ofMinutes(1));
        assertThat(pipelineService.read("batch", List.of("a", "b")))
                .containsEntry("a", "1")
                .containsEntry("b", "2");
    }

    @Test
    void luaRateLimiterIsAtomic() {
        String key = "rate-" + System.nanoTime();
        assertThat(rateLimiterService.tryAcquire(key, 3, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiterService.tryAcquire(key, 3, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiterService.tryAcquire(key, 3, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiterService.tryAcquire(key, 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void luaStockDeductionNeverOversells() throws Exception {
        String sku = "sku-" + System.nanoTime();
        stockService.init(sku, 10);
        int attempts = 20;
        AtomicInteger success = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                try {
                    stockService.deduct(sku);
                    success.incrementAndGet();
                } catch (IllegalStateException expected) {
                    // sold out
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();
        assertThat(success.get()).isEqualTo(10);
        assertThat(redisTemplate.opsForValue().get("ecology:redis:stock:" + sku)).isEqualTo("0");
    }

    @Test
    void stockMissingAndExhaustedAreDistinctErrors() {
        String sku = "missing-" + System.nanoTime();
        assertThatThrownBy(() -> stockService.deduct(sku))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not initialized");

        stockService.init(sku, 1);
        assertThat(stockService.deduct(sku)).isZero();
        assertThatThrownBy(() -> stockService.deduct(sku))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sold out");
    }

    @Test
    void redisTransactionWritesBothKeys() {
        List<Object> result = transactionService.multiSet("tx:a", "1", "tx:b", "2");
        assertThat(result).hasSize(2);
        assertThat(redisTemplate.opsForValue().get("tx:a")).isEqualTo("1");
        assertThat(redisTemplate.opsForValue().get("tx:b")).isEqualTo("2");
    }

    @Test
    void httpEndpointsExposeRedisCapabilities() throws Exception {
        String name = "http-" + System.nanoTime();
        String response = mockMvc.perform(post("/api/redis/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"priceCents\":4500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(name))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();

        mockMvc.perform(get("/api/redis/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name));

        mockMvc.perform(post("/api/redis/rate-limit/http-" + System.nanoTime())
                        .param("limit", "1")
                        .param("windowSeconds", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        String sku = "http-stock-" + System.nanoTime();
        mockMvc.perform(post("/api/redis/stock/" + sku + "/init").param("stock", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/redis/stock/" + sku + "/deduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
        mockMvc.perform(post("/api/redis/stock/" + sku + "/deduct"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
