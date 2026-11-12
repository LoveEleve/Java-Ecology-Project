# java-ecology-webflux

WebFlux 传输模块：Reactor Netty、Mono/Flux、SSE、函数式路由、WebClient、超时/错误处理。

## 定位

覆盖响应式 HTTP 的核心使用面：

- `Mono`：单个异步结果
- `Flux`：流式结果
- SSE：`text/event-stream`，事件 id/type/data
- 函数式路由：`RouterFunction` + `HandlerFunction`
- `WebClient`：Reactor Netty 连接池、连接/读超时、响应超时
- 背压：Reactive Streams 按需拉取
- 超时：Reactor `timeout` + 全局异常映射
- 错误处理：`ProblemDetail`
- Reactor Netty 连接/空闲超时
- Actuator

不包含：WebSocket（独立模块）、HTTP/2 + TLS、Spring Security、响应式数据库/R2DBC、真实上游服务。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | WebFlux、Actuator |
| Reactor Netty | HTTP server/client |
| Reactor | Mono/Flux/背压 |
| WebTestClient | 随机端口集成测试 |

## 目录结构

```text
java-ecology-webflux
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/webflux
   │  ├─ WebFluxApplication.java
   │  ├─ controller/ReactiveController.java
   │  ├─ router/FunctionalRoutes.java
   │  ├─ config/WebClientConfiguration.java
   │  └─ config/GlobalExceptionHandler.java
   ├─ main/resources/application.yml
   └─ test/java/...（WebTestClient + WebClient）
```

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/reactive/hello` | Mono 单值 |
| GET | `/api/reactive/items` | Flux JSON 流 |
| GET | `/api/reactive/sse` | SSE 3 个事件 |
| POST | `/api/reactive/echo` | Mono 请求/响应 |
| GET | `/api/reactive/timeout/{millis}` | 100ms 超时，504 |
| GET | `/functional/ping` | 函数式路由 |
| GET | `/functional/echo/{value}` | 路径参数 |
| POST | `/functional/validate` | 函数式校验 |
| GET | `/actuator/health` | 健康检查 |

### SSE

```bash
curl -N http://localhost:8108/api/reactive/sse
```

返回：

```text
id:1
event:tick
data:tick-0-...
```

### 超时

```bash
curl -i http://localhost:8108/api/reactive/timeout/500
# HTTP/1.1 504 Gateway Timeout
```

## 配置

```yaml
spring:
  codec:
    max-in-memory-size: 1MB
server:
  port: 8108
  netty:
    connection-timeout: 2s
    idle-timeout: 30s
```

WebClient：

- connect timeout 2s
- response timeout 3s
- read timeout 3s
- max in-memory 1MB

## 运行与测试

```bash
export JAVA_HOME=/env/javaEnv/jdk17
./mvnw -pl java-ecology-webflux spring-boot:run
```

```bash
./mvnw -pl java-ecology-webflux test
```

当前 7 个测试：

- Mono 单值
- Flux JSON 列表
- POST echo + Reactor timeout → 504 ProblemDetail
- SSE 事件数
- WebClient 解析 SSE 的 id/event/data
- 函数式路由 + 函数式校验
- WebClient 调用本机 Reactor Netty Server

测试使用随机端口，不依赖外部服务。

## 生产注意事项

- **阻塞调用不能进 EventLoop**：JDBC/Redis/文件 IO 要切到 boundedElastic 或独立线程池；否则会拖垮 Reactor Netty。
- **背压不是自动限流**：下游消费慢时需要 `limitRate`、超时、丢弃/缓冲策略，避免内存堆积。
- **SSE 长连接**：代理/LB idle timeout 要大于心跳间隔；客户端断开要能取消上游 Flux。
- **WebClient 必须设超时**：连接、响应、读取、整体调用都要有上限。
- **错误响应统一**：WebFlux 的 `ProblemDetail`/异常处理器要覆盖 `ResponseStatusException`、超时、上游 5xx。
- **HTTP/2 需要 TLS/ALPN 或 h2c**：本模块只跑 HTTP/1.1。
- **最大内存**：`spring.codec.max-in-memory-size` 防止大 body 打爆堆。
- **不要用 WebFlux 包阻塞事务**：事务/DB 场景优先确认驱动是响应式的（R2DBC）；否则用 MVC + 线程池更简单。
- **监控**：接入 observability 模块的 metrics/tracing，观察 EventLoop 队列、连接数、SSE 活跃数。

## 未覆盖/未验证

- 未做 HTTP/2/TLS、Spring Security、R2DBC、真实上游服务。
- 未做 SSE 断连/背压/海量长连接压测。
- 未做 EventLoop 阻塞检测和 Reactor Netty 深度调优。

## 参考

- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Reactor Netty](https://projectreactor.io/docs/netty/release/reference/)
- 本项目根 README 的统一模块约定
