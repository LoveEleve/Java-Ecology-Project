# java-ecology-redis

Spring Data Redis + Lettuce 的生产用法模块：序列化、Spring Cache、常用数据结构、Lua 原子脚本、Pipeline、TTL 和 Cluster 配置。

## 定位

覆盖研发/生产中的高频与中频场景：

- `RedisTemplate<String,Object>`：String key + JSON value 序列化
- Spring Cache：`@Cacheable` / `@CachePut` / `@CacheEvict`，按 cache 设置 TTL
- 常用结构：String 计数器、Hash、ZSet 排行榜
- Lua：固定窗口限流、库存原子扣减
- Pipeline：批量读写，减少 RTT
- 键规范：统一 `ecology:redis:` 命名空间，拒绝非法 key part
- Actuator：Redis health + 连接池指标
- Cluster：`cluster` profile 单独配置，生产按集群接入

不包含：分布式锁、看门狗、公平锁/读写锁、延迟队列。这些由 `java-ecology-redisson` 覆盖。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | Web、Cache、Actuator |
| Spring Data Redis | RedisTemplate、Cache、Lua、Pipeline |
| Lettuce | Redis 客户端，Boot 默认 |
| commons-pool2 | Lettuce 连接池 |
| Redis 7 | 本地与测试环境 |

## 目录结构

```text
java-ecology-redis
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java/com/loveeleve/ecology/redis
   │  │  ├─ RedisApplication.java
   │  │  ├─ common/          ApiResponse
   │  │  ├─ config/          RedisConfig、RedisKeys
   │  │  ├─ controller/      RedisController
   │  │  ├─ dto/             CreateProductRequest、ProductResponse
   │  │  ├─ exception/       ProductNotFoundException、GlobalExceptionHandler
   │  │  ├─ model/           Product
   │  │  └─ service/         Cache、Counter、Hash、ZSet、Lua、Pipeline
   │  └─ resources
   │     ├─ application.yml
   │     ├─ application-dev.yml
   │     └─ application-cluster.yml
   └─ test/java/com/loveeleve/ecology/redis
      ├─ RedisTestServer.java
      └─ RedisIntegrationTests.java
```

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/redis/products` | 创建并写入缓存 |
| GET | `/api/redis/products/{id}` | 缓存读取 |
| DELETE | `/api/redis/products/{id}` | 删除并清理缓存 |
| POST/GET | `/api/redis/counter/{name}` | String 计数器 |
| POST | `/api/redis/hash/{name}` | Hash 写入/读取 |
| POST/GET | `/api/redis/leaderboard/{board}` | ZSet 排行榜 |
| POST | `/api/redis/rate-limit/{key}` | Lua 固定窗口限流 |
| POST | `/api/redis/stock/{sku}/init` | 初始化库存 |
| POST | `/api/redis/stock/{sku}/deduct` | Lua 原子扣库存 |
| POST | `/api/redis/pipeline/{name}` | Pipeline 批量写读 |
| GET | `/actuator/health` | Redis 健康检查 |

## 配置说明

### 单机 / 本地

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      connect-timeout: 3s
      timeout: 3s
      lettuce:
        pool:
          enabled: true
          max-active: 16
          max-idle: 8
          min-idle: 2
          max-wait: 3s
```

### Cluster

```bash
java -jar java-ecology-redis/target/java-ecology-redis.jar \
  --spring.profiles.active=cluster
```

`application-cluster.yml` 使用 `spring.data.redis.cluster.nodes`，默认节点只是示例，生产必须替换。

### 缓存

| cache | TTL | 用途 |
|---|---|---|
| `product` | 5 分钟 | 商品详情 |
| `hot` | 1 分钟 | 热点数据 |
| 默认 | 10 分钟 | 其他 cache |

缓存 key 前缀：`ecology:redis:cache:<cacheName>::<key>`。

### Key 规范

业务 key 统一 `ecology:redis:` 前缀：

```text
ecology:redis:cache:product::1
ecology:redis:counter:visits
ecology:redis:hash:user:1
ecology:redis:zset:score
ecology:redis:rate:login:127.0.0.1
ecology:redis:stock:sku-1
ecology:redis:pipeline:batch:a
```

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 本地需要 Redis
redis-server --port 6379

# 单机
./mvnw -pl java-ecology-redis spring-boot:run

# Cluster
./mvnw -pl java-ecology-redis spring-boot:run \
  -Dspring-boot.run.profiles=cluster
```

## 验证

```bash
# 商品缓存
curl -s -X POST http://localhost:8085/api/redis/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"monitor","priceCents":129900}'
curl -s http://localhost:8085/api/redis/products/3

# 计数器
curl -s -X POST http://localhost:8085/api/redis/counter/visits
curl -s http://localhost:8085/api/redis/counter/visits

# Lua 限流：limit=2
curl -s -X POST 'http://localhost:8085/api/redis/rate-limit/login?limit=2&windowSeconds=60'
curl -s -X POST 'http://localhost:8085/api/redis/rate-limit/login?limit=2&windowSeconds=60'
curl -s -X POST 'http://localhost:8085/api/redis/rate-limit/login?limit=2&windowSeconds=60'

# Lua 库存
curl -s -X POST 'http://localhost:8085/api/redis/stock/sku-1/init?stock=10'
curl -s -X POST http://localhost:8085/api/redis/stock/sku-1/deduct

# 健康检查
curl -s http://localhost:8085/actuator/health
```

测试会自行启动一个临时 `redis-server` 进程，不依赖 6379 端口；本机必须安装 `redis-server`。

## 生产注意事项

- **不要用 JDK 序列化**：默认 `JdkSerializationRedisSerializer` 会产生不可读、跨语言不兼容的字节；本模块用 String key + JSON value。JSON 反序列化要限制可信来源，避免 default typing 安全风险。
- **缓存空值要显式设计**：本模块 `disableCachingNullValues()`，避免“缓存穿透保护”被误解；需要防穿透时用短 TTL 空值、布隆过滤器或互斥重建。
- **TTL 必须有**：无 TTL 的缓存会永久占用内存；按业务设置不同 cache TTL，热点短、基础数据长。
- **Lua 只做单 key 原子操作**：Redis Cluster 下多 key 脚本要求同 slot；本模块限流和库存都是单 key，不涉及 CROSSSLOT。多 key 场景要用 hash tag。
- **固定窗口限流有边界突刺**：窗口切换瞬间可能放过 2 倍流量；需要平滑限流用滑动窗口/令牌桶，或 Redisson `RRateLimiter`。
- **库存扣减必须原子**：`GET` 后 `DECR` 不是原子操作；必须用 Lua 或 `DECR` 后判断并回补，推荐 Lua。
- **Pipeline 不是事务**：批量命令减少 RTT，但中间失败不会自动回滚；需要事务用 Lua/MULTI，跨服务一致性用业务补偿。
- **Cluster 客户端要处理重定向**：`max-redirects`、节点发现、拓扑刷新要按客户端版本配置；单机配置不能直接当生产集群基线。
- **连接池与超时要分层**：Lettuce pool、命令 timeout、连接 timeout、业务超时要分别设置；Redis 慢会反压线程池。
- **缓存一致性**：先更新 DB 再删缓存，或延迟双删；不要假设 `@CacheEvict` 能解决并发读写竞态。
- **分布式锁不在这里**：Redisson 模块单独讲锁续期、看门狗、可重入和 RedLock 边界。

## 参考

- [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/)
- [Redis Lua](https://redis.io/docs/latest/develop/interact/programmability/eval-intro/)
- [Lettuce](https://lettuce.io/)
- 本项目根 README 的统一模块约定
