# java-ecology-observability

可观测性模块：Micrometer 指标、Prometheus 暴露、OpenTelemetry Tracing、TraceId 日志、健康检查与 Info。

## 定位

覆盖生产可观测性的最小完整链路：

- Metrics：Counter、Timer、DistributionSummary、业务自定义指标
- 注解指标：`@Timed`、`@Counted`
- Observation：`@Observed` + `ObservationRegistry`
- Prometheus：`/actuator/prometheus`
- Tracing：Micrometer Tracing + OpenTelemetry + OTLP
- 传播协议：W3C `traceparent`
- 日志关联：`traceId` / `spanId` 写入 MDC 和日志 pattern
- Health：内置 health + 自定义 `orderHealth` + liveness/readiness
- Info：应用信息、版本口径
- JVM、HTTP、Tomcat 等 Boot 自动指标

不包含：Grafana Dashboard、Prometheus Server、OTLP Collector、日志采集/Loki、告警规则；这些是基础设施侧。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web、Actuator、AOP |
| Micrometer | 指标与 Observation |
| Micrometer Tracing | 桥接 OpenTelemetry |
| OpenTelemetry SDK | OTLP 导出 |
| Prometheus Registry | `/actuator/prometheus` |
| JUnit 5 | 单元/上下文测试 |

## 目录结构

```text
java-ecology-observability
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/observability
   │  ├─ ObservabilityApplication.java
   │  ├─ config/ObservabilityConfiguration.java
   │  ├─ controller/ObservabilityController.java
   │  ├─ health/OrderHealthIndicator.java
   │  ├─ metrics/OrderMetrics.java
   │  └─ service/ObservedOrderService.java
   ├─ main/resources/application.yml
   ├─ main/resources/logback-spring.xml
   └─ test/java/...（指标 + 上下文 + Actuator）
```

## 核心实现

### 自定义指标

`OrderMetrics` 注册：

| 指标 | 类型 | 说明 |
|---|---|---|
| `demo.orders.created` | Counter | 成功订单数 |
| `demo.orders.failed` | Counter | 失败订单数 |
| `demo.orders.latency` | Timer | 订单处理延迟，P50/P95/P99 + histogram |
| `demo.orders.amount` | DistributionSummary | 订单金额分布 |

### 注解指标

`ObservedOrderService.process`：

- `@Timed("demo.order.timed")`：方法耗时
- `@Counted("demo.order.counted")`：调用次数
- `@Observed(name="demo.order.observed")`：Observation，自动产生指标和 trace span

### Tracing

- OTLP endpoint：`OTLP_ENDPOINT`，默认 `http://127.0.0.1:4318/v1/traces`
- 采样率：`TRACING_SAMPLE_RATE`，默认 `1.0`
- 传播：W3C
- 本模块只负责产生/导出 trace，不包含 Collector 和 UI

### 日志关联

`logging.pattern.level` 已注入：

```text
[application,traceId,spanId]
```

有 trace 时日志会看到实际 ID；测试中 tracing 关闭，所以显示为空。

## 接口与 Actuator

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/observability/orders/{orderId}` | 产生指标/Observation |
| GET | `/api/observability/slow` | 慢请求，供 HTTP 指标观察 |
| GET | `/actuator/health` | 健康检查，含 `orderHealth` |
| GET | `/actuator/health/liveness` | liveness |
| GET | `/actuator/health/readiness` | readiness |
| GET | `/actuator/info` | 应用信息 |
| GET | `/actuator/metrics` | 指标清单 |
| GET | `/actuator/prometheus` | Prometheus 格式 |

## 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
        demo.order.timed: true
  tracing:
    enabled: ${TRACING_ENABLED:true}
    sampling:
      probability: ${TRACING_SAMPLE_RATE:1.0}
    propagation:
      type: w3c
  otlp:
    tracing:
      endpoint: ${OTLP_ENDPOINT:http://127.0.0.1:4318/v1/traces}
  prometheus:
    metrics:
      export:
        enabled: true
```

本地没有 Collector 时建议：

```bash
export TRACING_ENABLED=false
```

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
./mvnw -pl java-ecology-observability spring-boot:run
```

验证：

```bash
curl -s http://localhost:8103/api/observability/orders/order-1
curl -s http://localhost:8103/actuator/health
curl -s http://localhost:8103/actuator/prometheus | grep demo_
```

Prometheus 抓取配置（示意）：

```yaml
scrape_configs:
  - job_name: java-ecology-observability
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["127.0.0.1:8103"]
```

## 测试

```bash
./mvnw -pl java-ecology-observability test
```

当前 4 个测试：

- `OrderMetrics`：成功/失败/延迟/金额
- Actuator health/info（含自定义 `orderHealth`）
- Prometheus 端点：自定义指标、`@Timed`、`@Counted`、`@Observed`、JVM 指标
- `/actuator/metrics` 指标清单

测试关闭 tracing，不启动 Collector、Prometheus 或 Grafana。

## 生产注意事项

- **指标标签不能高基数**：不要用 orderId、userId、traceId 做 tag；用少量稳定维度。
- **Prometheus 端点要保护**：生产只对监控网段开放，不要暴露公网。
- **采样率按流量调**：1.0 只适合低流量/排障；高流量用 1%–10% 或尾部采样。
- **OTLP 导出异步**：Collector 不可用时避免阻塞业务；配置队列/超时/重试并监控丢 span。
- **日志量**：traceId/spanId 关联不等于全量 JSON 日志；生产要控制级别、采样和采集侧成本。
- **Health 细节**：`show-details=always` 仅适合内网；公网只返回状态。
- **HTTP 指标路径归一化**：`/orders/{id}` 这类模板路径由 Boot 处理，避免把 ID 打成 tag。
- **Timed/Observed 会叠加**：同时用时确认不会重复统计同一语义指标。
- **Actuator 不是业务 API**：单独端口/鉴权/网络策略，避免和业务流量混用。

## 未覆盖/未验证

- 未启动 Prometheus Server、Grafana、OTLP Collector、Loki。
- 未做真实 trace 导出/采样/Collector 故障注入。
- 未做告警规则、SLO、Dashboard。
- 未做日志采集端到端验证。

## 参考

- [Spring Boot Observability](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)
- [Micrometer](https://micrometer.io/)
- [OpenTelemetry](https://opentelemetry.io/)
