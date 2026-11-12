# java-ecology-redisson

Redisson 的生产用法模块：分布式锁、看门狗、公平锁/读写锁、限流、信号量和带 TTL 的 MapCache。

## 定位

Redisson 不是 Spring Data Redis 的替代品，而是另一个 Redis 客户端，强项是**分布式并发原语**：

- `RLock`：可重入分布式锁
- `lock()`：不指定 leaseTime 时由看门狗续期
- `tryLock(waitTime, leaseTime)`：显式租约，不启用看门狗
- `RFairLock`：按请求顺序排队
- `RReadWriteLock`：读写锁
- `RRateLimiter`：分布式限流
- `RSemaphore`：分布式信号量
- `RMapCache`：带 TTL 的分布式 Map

不包含 Spring Data Redis 的 Cache/Lua/Pipeline 示例；那些在 `java-ecology-redis`。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | Web、Actuator、Redis 属性 |
| Redisson 3.27.0 | `redisson-spring-boot-starter` |
| Redis 7 | 本地与测试环境 |

## 目录结构

```text
java-ecology-redisson
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java/com/loveeleve/ecology/redisson
   │  │  ├─ RedissonApplication.java
   │  │  ├─ common/          ApiResponse
   │  │  ├─ config/          RedissonKeys
   │  │  ├─ controller/      RedissonController
   │  │  ├─ exception/       GlobalExceptionHandler
   │  │  └─ service/         Lock、RateLimiter、Semaphore、MapCache
   │  └─ resources
   │     ├─ application.yml
   │     ├─ application-dev.yml
   │     └─ application-cluster.yml
   └─ test/java/com/loveeleve/ecology/redisson
      ├─ RedissonTestServer.java
      └─ RedissonIntegrationTests.java
```

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/redisson/lock/{name}/increment` | 锁内读改写，演示串行化 |
| POST | `/api/redisson/try-lock/{name}` | 尝试获取锁，立即释放 |
| POST | `/api/redisson/fair-lock/{name}` | 公平锁尝试 |
| POST | `/api/redisson/read-write/{name}` | 写锁写入 |
| GET | `/api/redisson/read-write/{name}` | 读锁读取 |
| POST | `/api/redisson/rate-limit/{name}` | 分布式限流 |
| GET | `/api/redisson/rate-limit/{name}/available` | 剩余许可 |
| POST | `/api/redisson/semaphore/{name}/acquire` | 获取信号量 |
| POST | `/api/redisson/semaphore/{name}/release` | 释放信号量 |
| GET | `/api/redisson/semaphore/{name}/available` | 剩余许可 |
| POST | `/api/redisson/map/{name}` | 写入带 TTL 的 Map |
| GET | `/api/redisson/map/{name}` | 读取 Map |
| GET | `/actuator/health` | 健康检查 |

## 配置说明

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DATABASE:0}
      connect-timeout: 3s
      timeout: 3s
```

### 连接池与编码

`RedissonConfig` 对单机模式做了生产化调整：

| 配置 | 值 | 说明 |
|---|---|---|
| `codec` | `JsonJacksonCodec` | Redis 中存 JSON，便于排查 |
| `lockWatchdogTimeout` | `30s` | 看门狗续期周期 |
| `connectionMinimumIdleSize` | `2` | 最小空闲连接 |
| `connectionPoolSize` | `16` | 普通命令连接池 |
| `subscriptionConnectionPoolSize` | `4` | 订阅连接池 |
| `retryAttempts` | `3` | 命令重试次数 |
| `retryInterval` | `1500ms` | 重试间隔 |

Cluster 模式不在这里强制单机池参数，避免覆盖集群配置。

密码通过标准 Spring Boot 属性注入：

```bash
export SPRING_DATA_REDIS_PASSWORD=change-me
```

不要把空密码写成 `password: ""`：Redisson 会把它当成真实密码发送 `AUTH`，Redis 未配置密码时会报
`ERR AUTH <password> called without any password configured`。没有密码时直接省略该属性。

### Cluster

```bash
java -jar java-ecology-redisson/target/java-ecology-redisson.jar \
  --spring.profiles.active=cluster
```

`application-cluster.yml` 使用 `spring.data.redis.cluster.nodes`；生产节点必须替换为真实集群。

## 锁语义

```java
RLock lock = redissonClient.getLock("ecology:redisson:lock:order:1");
lock.lock();                 // 看门狗自动续期
try {
    // 临界区
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

| 用法 | 看门狗 | 适用 |
|---|---|---|
| `lock()` | 开启 | 临界区执行时间不确定，但必须有上限 |
| `tryLock(wait, lease, unit)` | 关闭 | 已知最长执行时间，租约要大于业务耗时 |
| `lock(leaseTime, unit)` | 关闭 | 明确租约，避免长期占用 |

**必须按持有线程解锁**：`isHeldByCurrentThread()` 是最后防线；不要把锁对象跨线程释放。

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 本地 Redis
redis-server --port 6379

# 单机
./mvnw -pl java-ecology-redisson spring-boot:run

# Cluster
./mvnw -pl java-ecology-redisson spring-boot:run \
  --spring.profiles.active=cluster
```

## 验证

```bash
# 锁内自增
curl -s -X POST http://localhost:8086/api/redisson/lock/order-1/increment

# 尝试锁
curl -s -X POST 'http://localhost:8086/api/redisson/try-lock/order-1?waitMillis=100&leaseMillis=3000'

# 公平锁
curl -s -X POST 'http://localhost:8086/api/redisson/fair-lock/order-1?waitMillis=100'

# 读写锁
curl -s -X POST 'http://localhost:8086/api/redisson/read-write/config?value=v1'
curl -s http://localhost:8086/api/redisson/read-write/config

# 限流：每 1 分钟 3 个
curl -s -X POST 'http://localhost:8086/api/redisson/rate-limit/login?permits=3&interval=1&unit=MINUTES'

# 信号量：2 个许可
curl -s -X POST 'http://localhost:8086/api/redisson/semaphore/workers/acquire?permits=2'
curl -s -X POST http://localhost:8086/api/redisson/semaphore/workers/release

# 带 TTL 的 Map
curl -s -X POST 'http://localhost:8086/api/redisson/map/session?key=u1&value=alice&ttlSeconds=60'
curl -s 'http://localhost:8086/api/redisson/map/session?key=u1'
```

测试会自动启动临时 `redis-server`，不依赖固定 6379 端口；本机必须安装 `redis-server`。当前测试覆盖：锁内并发读改写、跨线程 tryLock、公平锁/读写锁、限流、信号量、MapCache、HTTP 接口和 Redisson 客户端配置。

## 生产注意事项

- **锁的 key 要按业务粒度设计**：`order:1` 和 `order:2` 不应互相阻塞；大 key/热点锁会成为吞吐瓶颈。
- **RedLock 不是强一致银弹**：多 Redis 主节点 RedLock 仍有争议；对强一致要求高的场景优先数据库唯一约束/状态机/幂等表。
- **锁和事务顺序**：先拿锁再开事务，事务提交后再释放锁；不要在事务未提交时释放锁，否则其他线程可能读到旧状态。
- **看门狗不是无限续期**：JVM 停止、Redis 连接断开、线程死亡都会影响续期；leaseTime 模式要覆盖最长业务耗时。
- **`tryLock` 默认不等待**：无参 `tryLock()` 立即返回；需要等待时显式传 `waitTime`，并设置合理 `leaseTime`。
- **可重入是线程级**：同一线程重复 `lock()` 会增加计数；跨线程释放会失败，必须成对 `unlock()`。
- **限流配置只初始化一次**：`trySetRate` 对已存在的 limiter 不会覆盖旧配置；变更配置要换 key 或显式管理生命周期。
- **信号量许可要持久化设计**：`trySetPermits` 只在首次设置；应用重启不会自动重置，业务要明确初始化和回收策略。
- **Redisson 连接数不是越大越好**：默认连接池较大，按 QPS、命令耗时和 Redis 连接上限调整；连接风暴会拖垮 Redis。
- **Redis Cluster 多 key 要同 slot**：锁、限流、信号量通常是单 key；批量/多 key 操作要用 hash tag，避免 CROSSSLOT。
- **健康检查只代表连接可用**：Actuator UP 不代表锁/限流业务配置正确；关键路径要有超时、降级和告警。

## 参考

- [Redisson Wiki](https://github.com/redisson/redisson/wiki)
- [Redisson Spring Boot Starter](https://github.com/redisson/redisson/tree/master/redisson-spring-boot-starter)
- 本项目根 README 的统一模块约定
