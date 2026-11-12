# java-ecology-gateway

Spring Cloud Gateway 生产用法模块：Nacos 服务发现、LoadBalancer、Sentinel Gateway 流控、Redis RateLimiter、CORS、请求 ID 透传和超时/重试。

## 定位

Gateway 是**响应式 API 网关**，负责入口路由与横切治理：

- 按 Path 路由到 `lb://ecology-http-provider`
- Nacos 服务发现 + LoadBalancer
- Sentinel Gateway 路由级流控
- Redis `RequestRateLimiter` 限流
- CORS、请求 ID/TraceId、访问日志
- 超时、重试、连接池
- Tomcat 不参与，底层是 Netty/Reactor

不包含：JWT/OAuth2 认证、TLS 终止、Sentinel Dashboard 动态规则、Kubernetes Ingress 替代。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Actuator |
| Spring Cloud Gateway 4.1 | WebFlux 网关 |
| Spring Cloud Alibaba 2023.0.1.2 | Nacos Discovery、Sentinel |
| Sentinel Gateway Adapter | 路由级流控 |
| Spring Data Redis Reactive | Redis 令牌桶限流 |
| Nacos 2.x | 注册中心 |
| Redis | RateLimiter 存储 |

## 目录结构

```text
java-ecology-gateway
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/gateway
   │  ├─ GatewayApplication.java
   │  ├─ config/GatewayRouteConfig.java
   │  ├─ config/SentinelGatewayRules.java
   │  └─ filter/TraceGlobalFilter.java
   ├─ main/resources/application.yml
   ├─ main/resources/application-test.yml
   └─ test/java/.../GatewayContextTests.java
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| 路由转发 | `spring.cloud.gateway.routes` | `application.yml` | 本地 Nacos 手工验证 |
| Nacos + LB | `lb://ecology-http-provider` | `application.yml`、pom | 本地 Nacos 手工验证 |
| Sentinel 路由流控 | `GatewayRuleManager` | `config/SentinelGatewayRules.java` | 代码/配置 |
| Redis 限流 | `RequestRateLimiter` + `ipKeyResolver` | `application.yml`、`config/GatewayRouteConfig.java` | 本地 Redis 手工验证 |
| 请求 ID/TraceId | `TraceGlobalFilter` | `filter/TraceGlobalFilter.java` | 手工验证 |
| CORS | `globalcors` | `application.yml` | 代码/配置 |
| 超时/重试/连接池 | Gateway HTTP client + Retry filter | `application.yml` | 代码/配置 |

## 路由

```text
GET /api/downstream/**  ->  lb://ecology-http-provider
```

过滤器链：

1. `TraceGlobalFilter` 生成/透传 `X-Request-Id`、`X-Trace-Id`
2. `AddRequestHeader=X-Gateway: java-ecology`
3. Redis `RequestRateLimiter`：每 IP 20 QPS，burst 40
4. `Retry`：GET 对 502/503/504 重试 2 次
5. 转发到 Nacos 中的 Provider 实例

## 配置要求

```bash
export NACOS_SERVER_ADDR=127.0.0.1:8848
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
```

默认端口 `8096`。核心 YAML：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: http-provider
          uri: lb://ecology-http-provider
          predicates:
            - Path=/api/downstream/**
          filters:
            - AddRequestHeader=X-Gateway, java-ecology
            - name: RequestRateLimiter
              args:
                key-resolver: '#{@ipKeyResolver}'
                redis-rate-limiter.replenishRate: 20
                redis-rate-limiter.burstCapacity: 40
            - name: Retry
              args:
                retries: 2
                methods: GET
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE,GATEWAY_TIMEOUT
```

Sentinel 规则在 `SentinelGatewayRules`：路由 `http-provider` 100 QPS。生产动态规则使用 `--spring.profiles.active=sentinel-nacos`，从 Nacos `ECO_ECOLOGY_SENTINEL` 的 `java-ecology-gateway-gw-flow-rules` 加载并关闭本地规则。

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
export NACOS_SERVER_ADDR=127.0.0.1:8848

# 1. 启动 Provider
java -jar java-ecology-http-provider/target/java-ecology-http-provider.jar

# 2. 启动 Gateway
java -jar java-ecology-gateway/target/java-ecology-gateway.jar
```

## 验证

```bash
curl -s http://localhost:8096/actuator/health
curl -s 'http://localhost:8096/api/downstream/hello?name=gateway'
curl -s 'http://localhost:8096/api/downstream/echo?message=hello'
curl -s -D - 'http://localhost:8096/api/downstream/hello?name=trace' -o /dev/null
```

本机已用 Nacos 2.3.2 + Redis + Provider 手工验证路由和 Redis 限流；自动化测试只启动 Gateway 上下文，不启动 Nacos/Redis。

## 生产注意

- **Gateway 不要引入 spring-boot-starter-web**：Servlet 与 WebFlux 会冲突。
- **路由用服务名**：`lb://ecology-http-provider`，不要写死实例 IP。
- **限流 key 要选对**：当前优先取 `X-Forwarded-For`，必须只信任受控代理写入的头；按 IP 会被 NAT 聚合，按用户/租户/API Key 更可靠，但必须先认证。
- **Sentinel 规则落地**：代码内规则只用于演示，生产用 Dashboard/配置中心持久化到 Nacos。
- **Retry 要幂等**：只对 GET 或幂等接口重试；写操作重试会重复下单。
- **超时分层**：Gateway response-timeout、Provider 超时、下游超时要递减。
- **连接池/事件循环**：Netty 事件循环不要阻塞；不要在 GlobalFilter 做同步 IO。
- **CORS 白名单**：生产不要 `* + credentials`；按域名配置。
- **认证/TLS**：生产在 Gateway 做 JWT/OAuth2、TLS 终止、IP/租户 ACL。
- **可观测性**：请求 ID、访问日志、指标、链路追踪必须接入。

## 未覆盖/未验证

- 未接 Sentinel Dashboard 动态规则和规则持久化。
- 未做 JWT/OAuth2、TLS、租户级限流。
- 未做 Gateway 集群/多实例、压测和故障转移。
- 自动化测试不启动 Nacos/Redis。

## 参考

- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Sentinel Gateway](https://sentinelguard.io/zh-cn/docs/api-gateway-flow-control.html)
- [Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba)
- 本项目根 README 的统一模块约定
