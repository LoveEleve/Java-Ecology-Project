package com.loveeleve.ecology.redis.controller;

import com.loveeleve.ecology.redis.common.ApiResponse;
import com.loveeleve.ecology.redis.dto.CreateProductRequest;
import com.loveeleve.ecology.redis.dto.ProductResponse;
import com.loveeleve.ecology.redis.service.CounterService;
import com.loveeleve.ecology.redis.service.HashService;
import com.loveeleve.ecology.redis.service.LeaderboardService;
import com.loveeleve.ecology.redis.service.PipelineService;
import com.loveeleve.ecology.redis.service.ProductCacheService;
import com.loveeleve.ecology.redis.service.PubSubService;
import com.loveeleve.ecology.redis.service.RedisTransactionService;
import com.loveeleve.ecology.redis.service.RateLimiterService;
import com.loveeleve.ecology.redis.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/redis")
public class RedisController {

    private final ProductCacheService productCacheService;
    private final CounterService counterService;
    private final HashService hashService;
    private final LeaderboardService leaderboardService;
    private final RateLimiterService rateLimiterService;
    private final StockService stockService;
    private final PipelineService pipelineService;
    private final PubSubService pubSubService;
    private final RedisTransactionService redisTransactionService;

    public RedisController(ProductCacheService productCacheService,
                           CounterService counterService,
                           HashService hashService,
                           LeaderboardService leaderboardService,
                           RateLimiterService rateLimiterService,
                           StockService stockService,
                           PipelineService pipelineService,
                           PubSubService pubSubService,
                           RedisTransactionService redisTransactionService) {
        this.productCacheService = productCacheService;
        this.counterService = counterService;
        this.hashService = hashService;
        this.leaderboardService = leaderboardService;
        this.rateLimiterService = rateLimiterService;
        this.stockService = stockService;
        this.pipelineService = pipelineService;
        this.pubSubService = pubSubService;
        this.redisTransactionService = redisTransactionService;
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ProductResponse.from(productCacheService.create(request))));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable long id) {
        return ApiResponse.ok(ProductResponse.from(productCacheService.get(id)));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable long id) {
        productCacheService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/counter/{name}")
    public ApiResponse<Long> increment(@PathVariable String name) {
        return ApiResponse.ok(counterService.increment(name));
    }

    @GetMapping("/counter/{name}")
    public ApiResponse<String> counter(@PathVariable String name) {
        return ApiResponse.ok(counterService.get(name));
    }

    @PostMapping("/hash/{name}")
    public ApiResponse<Map<Object, Object>> putHash(@PathVariable String name,
                                                    @RequestBody Map<String, String> values) {
        hashService.putAll(name, values);
        return ApiResponse.ok(hashService.entries(name));
    }

    @PostMapping("/leaderboard/{board}")
    public ApiResponse<List<LeaderboardService.RankEntry>> addScore(@PathVariable String board,
                                                                    @RequestParam String member,
                                                                    @RequestParam double score) {
        leaderboardService.add(board, member, score);
        return ApiResponse.ok(leaderboardService.top(board, 10));
    }

    @GetMapping("/leaderboard/{board}")
    public ApiResponse<List<LeaderboardService.RankEntry>> leaderboard(@PathVariable String board,
                                                                        @RequestParam(defaultValue = "10") int n) {
        return ApiResponse.ok(leaderboardService.top(board, n));
    }

    @PostMapping("/rate-limit/{key}")
    public ApiResponse<Boolean> rateLimit(@PathVariable String key,
                                          @RequestParam(defaultValue = "5") int limit,
                                          @RequestParam(defaultValue = "60") long windowSeconds) {
        return ApiResponse.ok(rateLimiterService.tryAcquire(key, limit, Duration.ofSeconds(windowSeconds)));
    }

    @PostMapping("/stock/{sku}/init")
    public ApiResponse<String> initStock(@PathVariable String sku,
                                         @RequestParam long stock) {
        stockService.init(sku, stock);
        return ApiResponse.ok("initialized");
    }

    @PostMapping("/stock/{sku}/deduct")
    public ApiResponse<Long> deductStock(@PathVariable String sku) {
        return ApiResponse.ok(stockService.deduct(sku));
    }

    @PostMapping("/pubsub")
    public ApiResponse<Long> publish(@RequestParam String message) {
        return ApiResponse.ok(pubSubService.publish(message));
    }

    @PostMapping("/transaction")
    public ApiResponse<List<Object>> transaction(@RequestParam String key1,
                                                 @RequestParam String value1,
                                                 @RequestParam String key2,
                                                 @RequestParam String value2) {
        return ApiResponse.ok(redisTransactionService.multiSet(key1, value1, key2, value2));
    }

    @PostMapping("/pipeline/{name}")
    public ApiResponse<Map<String, String>> pipeline(@PathVariable String name,
                                                     @RequestBody Map<String, String> values) {
        pipelineService.write(name, values, Duration.ofMinutes(5));
        return ApiResponse.ok(pipelineService.read(name, List.copyOf(values.keySet())));
    }
}
