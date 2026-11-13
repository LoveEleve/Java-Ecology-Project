# java-ecology-skywalking

SkyWalking APM 接入模块：Java Agent、`@Trace` 自定义 Span、Tag、TraceId 日志、OAP 配置说明。

## 定位

覆盖 SkyWalking 生产接入的关键点：

- `-javaagent` 挂载 SkyWalking Agent
- Agent 服务名 / OAP 后端地址
- `@Trace` 自定义 Span
- `ActiveSpan.tag` 业务标签
- `TraceContext.traceId()` 查询 traceId
- Logback `%tid` 日志关联
- HTTP/JDBC/Redis 等自动探针（由 Agent 提供，不是业务代码）
- 无 Agent 时业务可正常降级运行

不包含：SkyWalking OAP/UI 部署、告警规则、采样策略调优、跨线程上下文传播、Agent 插件开发。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web/Actuator |
| SkyWalking Java Agent 9.6.0 | 运行时探针 |
| apm-toolkit-trace | `@Trace` / `ActiveSpan` / `TraceContext` |
| apm-toolkit-logback-1.x | `%tid` 日志布局 |
| JUnit 5 | 无 Agent 上下文/业务测试 |

## 目录结构

```text
java-ecology-skywalking
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/skywalking
   │  ├─ SkyWalkingApplication.java
   │  ├─ service/TracedOrderService.java
   │  └─ controller/TracedOrderController.java
   ├─ main/resources/application.yml
   ├─ main/resources/logback-spring.xml
   └─ test/java/...（业务 + 上下文）
```

## Agent 挂载（必须）

Agent 不在 Maven 依赖里，必须从 SkyWalking 发行包获取：

```bash
java -javaagent:/opt/skywalking/skywalking-agent/skywalking-agent.jar \
  -Dskywalking.agent.service_name=java-ecology-skywalking \
  -Dskywalking.collector.backend_service=oap-1:11800,oap-2:11800 \
  -jar java-ecology-skywalking.jar
```

或者环境变量：

```bash
export SW_AGENT_NAME=java-ecology-skywalking
export SW_AGENT_COLLECTOR_BACKEND_SERVICES=oap-1:11800,oap-2:11800
```

**不挂 Agent 时**：

- `@Trace` 不会产生真实 Span
- `ActiveSpan.tag` 是 no-op
- `TraceContext.traceId()` 返回 `N/A`
- 业务代码仍可正常运行

## 自定义 Span

`TracedOrderService`：

```java
@Trace(operationName = "order.process")
public String process(String orderId, BigDecimal amount) {
    ActiveSpan.tag("order.id", orderId);
    ActiveSpan.tag("order.amount", String.valueOf(amount));
    return "ok:" + orderId;
}
```

- `@Trace` 创建本地 Span
- `@Tag` 添加静态低基数标签
- `ActiveSpan.tag` 添加动态低基数标签
- 业务异常用 `ActiveSpan.error` 标记
- 不要用 userId/orderId 作为大规模 tag（高基数）
- 跨线程池需要 `TraceCrossThread` 或手动 Context 传播；本示例没有做

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/skywalking/orders/{orderId}?amount=100` | 自定义 span + risk check |
| GET | `/api/skywalking/trace-id` | 返回当前 traceId |
| GET | `/actuator/health` | 健康检查 |

无 Agent 时 `traceId` 为 `N/A`，挂 Agent 后为真实链路 ID。

## 日志关联

`logback-spring.xml` 使用 SkyWalking layout：

```xml
<layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout">
    <pattern>%d ... [%tid] %logger{36} - %msg%n</pattern>
</layout>
```

无 Agent 输出：

```text
[TID: N/A]
```

有 Agent 后输出真实 traceId，便于日志平台按链路聚合。

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
./mvnw -pl java-ecology-skywalking spring-boot:run
```

挂 Agent 的启动方式见上面的 `-javaagent` 命令。

## 测试

```bash
./mvnw -pl java-ecology-skywalking test
```

当前 5 个测试：

- 无 Agent 时 `@Trace` 方法业务可调用
- `@Trace` operationName / `@Tag` 注解存在
- 非法订单返回业务异常，业务可继续
- `TraceContext.traceId()` 可查询（无 Agent 为 N/A）
- Spring 上下文 + Logback `%tid` 布局

测试不启动 Agent/OAP/UI。

## 生产注意事项

- **Agent 必须挂**：只在 pom 里加 toolkit 不会自动采集 HTTP/JDBC/Redis。
- **服务名稳定**：`SW_AGENT_NAME` 要与监控平台服务目录一致，不要按 Pod 随机生成。
- **OAP 集群**：`backend_service` 配置多个 OAP，Agent 做负载/故障切换。
- **采样**：高流量开 `agent.sample_n_per_3_secs` 或采样率，避免全量 Span 压垮 OAP。
- **高基数 Tag**：只放低基数维度；不要把 orderId、userId、traceId 当 tag。
- **跨线程传播**：线程池/CompletableFuture 需要手动传递 SkyWalking Context，否则链路断开。
- **日志成本**：`%tid` 很有用，但日志量仍要按级别/采样控制。
- **Agent 版本匹配**：Agent、OAP、UI 版本要对齐；升级先在预发验证插件兼容性。
- **不要依赖 Agent 做业务**：Agent 故障时业务应继续，不能把 trace 采集当强依赖。

## 未覆盖/未验证

- 未部署 SkyWalking Agent/OAP/UI，未做真实链路采集。
- 未做跨线程上下文传播、采样、告警、Agent 插件兼容性测试。
- 未做高流量 Span 压测。

## 参考

- [SkyWalking](https://skywalking.apache.org/)
- [SkyWalking Java Agent](https://skywalking.apache.org/docs/skywalking-java/latest/readme/)
- 本项目根 README 的统一模块约定
