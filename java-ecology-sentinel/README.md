# java-ecology-sentinel

Sentinel 生产用法模块：流控、熔断降级、热点参数、系统保护、授权规则，以及注解 fallback/blockHandler 和 Nacos 动态规则。

## 定位

覆盖生产稳定性最常用的 Sentinel 能力：

- 流控：QPS、线程数、预热、排队等待
- 熔断：慢调用比例、异常比例
- 热点参数：按参数值限流
- 系统保护：全局 QPS 等系统规则
- 授权：按调用来源白名单/黑名单
- 注解：`@SentinelResource` 的 `blockHandler`、`fallback`、`exceptionsToIgnore`
- Web 适配：URL 资源、URL 归一化、来源解析、统一 429 JSON
- 规则持久化：Nacos DataSource（可选）
- Dashboard：通过 `SENTINEL_DASHBOARD` 接入

不包含：Sentinel Cluster 流控（需要独立 token server）、Dashboard 持久化改造、网关流控（已在 Gateway 模块覆盖）。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web、Actuator |
| Spring Cloud Alibaba 2023.0.1.2 | Sentinel 自动配置 |
| Sentinel 1.8.8 | core、webmvc v6x adapter、parameter flow |
| sentinel-datasource-nacos | 动态规则 |
| JUnit 5 | 无外部依赖单测 |

## 目录结构

```text
java-ecology-sentinel
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/sentinel
   │  ├─ SentinelApplication.java
   │  ├─ config/SentinelDemoRuleConfiguration.java
   │  ├─ config/SentinelNacosProperties.java
   │  ├─ config/SentinelNacosRuleConfiguration.java
   │  ├─ config/SentinelWebConfiguration.java
   │  ├─ controller/SentinelController.java
   │  ├─ service/FlowControlService.java
   │  ├─ service/DegradeService.java
   │  ├─ service/HotParamService.java
   │  └─ service/AuthorityService.java
   ├─ main/resources/application.yml
   ├─ main/resources/application-nacos.yml
   └─ test/java/...（流控/熔断/热点/授权/Web 适配/上下文）
```

## 核心能力

| 能力 | 入口 | 规则资源 | 说明 |
|---|---|---|---|
| QPS 流控 | `/api/sentinel/flow/order` | `flow-order` | 5 QPS，排队 200 ms |
| 预热流控 | `/api/sentinel/flow/warmup` | `flow-warmup` | 5 QPS，10 秒预热 |
| 线程数流控 | `/api/sentinel/flow/thread` | `flow-thread` | 2 个并发线程 |
| 慢调用熔断 | `/api/sentinel/degrade/slow` | `degrade-order` | RT > 200 ms，慢调用比例 50%，窗口 10 s |
| 异常比例熔断 | `/api/sentinel/degrade/exception` | `degrade-exception` | 异常比例 50%，窗口 10 s |
| 热点参数 | `/api/sentinel/hot?userId=...` | `hot-param` | 同一 userId 每秒 2 次 |
| 授权 | `/api/sentinel/authority` | `authority-order` | 白名单 `trusted-client` |
| URL 归一化 | `/api/sentinel/orders/{orderId}` | URL 资源 | 数字 ID 归一为 `{orderId}` |
| 规则查询 | `/api/sentinel/rules` | 全局 | 只读查看当前规则 |
| 健康检查 | `/actuator/health` | — | Actuator |

### 注解行为

- `@SentinelResource(value = ..., blockHandler = ...)`：被规则拦截时调用 blockHandler，返回业务降级结果。
- `@SentinelResource(value = ..., fallback = ...)`：业务异常且开启熔断时调用 fallback。
- `exceptionsToIgnore = IllegalArgumentException.class`：参数错误直接抛出，不进入 fallback，也不计入熔断。
- `FlowControlService` 的 `flow-thread` 是线程数流控演示；生产应把 blockHandler 的返回统一映射为业务错误码/HTTP 状态。

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/sentinel/flow/order?userId=u1` | QPS 流控 |
| GET | `/api/sentinel/flow/warmup?userId=u1` | 预热 |
| GET | `/api/sentinel/flow/thread?millis=500` | 线程数流控 |
| GET | `/api/sentinel/degrade/slow?delayMs=300` | 慢调用熔断 |
| GET | `/api/sentinel/degrade/exception` | 异常比例熔断 |
| GET | `/api/sentinel/hot?userId=u1` | 热点参数 |
| GET | `/api/sentinel/authority` | 授权（需 `X-Origin: trusted-client`） |
| GET | `/api/sentinel/orders/10086` | URL 归一化 |
| GET | `/api/sentinel/rules` | 查看规则 |

## 配置

默认使用进程内演示规则，不依赖外部服务：

```yaml
app:
  sentinel:
    demo-rules: true
    nacos:
      enabled: false

spring:
  cloud:
    sentinel:
      transport:
        dashboard: ${SENTINEL_DASHBOARD:}
        port: ${SENTINEL_TRANSPORT_PORT:8719}
        eager: false
```

### 接入 Dashboard

```bash
export SENTINEL_DASHBOARD=127.0.0.1:8858
./mvnw -pl java-ecology-sentinel spring-boot:run
```

Dashboard 默认把规则存在内存；生产应使用 Nacos 持久化，避免重启丢失。

### 接入 Nacos 动态规则

启用 `nacos` profile：

```bash
export NACOS_SERVER=127.0.0.1:8848
./mvnw -pl java-ecology-sentinel spring-boot:run -Dspring-boot.run.profiles=nacos
```

`application-nacos.yml` 会关闭进程内演示规则，并注册五类 DataSource：

| 规则 | Data ID | JSON 类型 |
|---|---|---|
| 流控 | `java-ecology-sentinel-flow-rules` | `List<FlowRule>` |
| 熔断 | `java-ecology-sentinel-degrade-rules` | `List<DegradeRule>` |
| 热点 | `java-ecology-sentinel-param-flow-rules` | `List<ParamFlowRule>` |
| 系统 | `java-ecology-sentinel-system-rules` | `List<SystemRule>` |
| 授权 | `java-ecology-sentinel-authority-rules` | `List<AuthorityRule>` |

Nacos 配置示例（流控）：

```json
[
  {
    "resource": "flow-order",
    "limitApp": "default",
    "grade": 1,
    "count": 5,
    "strategy": 0,
    "controlBehavior": 2,
    "maxQueueingTimeMs": 200
  }
]
```

Nacos 集群地址用逗号分隔，例如 `nacos-1:8848,nacos-2:8848`；开启鉴权时通过 `NACOS_USERNAME` / `NACOS_PASSWORD` 或 `NACOS_ACCESS_KEY` / `NACOS_SECRET_KEY` 注入。

## 测试

```bash
./mvnw -pl java-ecology-sentinel test
```

当前 16 个测试覆盖：

- QPS 流控、线程数流控
- `@SentinelResource` blockHandler
- 慢调用/异常比例熔断、注解 fallback
- 热点参数限流
- 系统 QPS 规则（仅 inbound）
- 授权白名单
- URL 归一化、统一 429 JSON
- Spring 上下文启动（含 Web 拦截器、演示规则）
- MockMvc 授权、热点参数、URL 规则 429

测试不启动 Dashboard/Nacos，不依赖外部服务。

## 生产注意事项

- **规则来源要单一**：Dashboard 和 Nacos 同时改规则会互相覆盖；生产以 Nacos 为准，Dashboard 只做观测/临时调试。
- **`demo-rules` 只用于演示**：生产首启就应关闭，避免默认阈值误伤流量。
- **阈值必须有容量依据**：QPS/线程数要结合 Tomcat 线程池、下游容量、P99 RT；不要凭感觉设。
- **熔断参数要调**：`minRequestAmount` 太小会抖动，`statIntervalMs` 太小会放大偶发异常，`timeWindow` 太短会频繁半开。
- **热点参数有内存成本**：参数值高基数会放大统计开销，必要时配置参数例外项/白名单。
- **系统规则是全局兜底且只检查 inbound**：QPS/线程/负载阈值按机器规格设置，别和单资源流控重复叠加；出站调用不会被系统规则保护。
- **授权不是认证**：`X-Origin` 只是来源标识，生产要由网关/认证层写入，不能信任客户端任意传。
- **集群流控需要独立 token server**：本模块是单机规则；多实例总 QPS 限流要接 Sentinel Cluster。
- **注解 fallback 不是所有异常都兜**：`exceptionsToIgnore`、`exceptionsToTrace` 要按业务配置；fallback 内不能再调同一资源。
- **Web 统一响应**：模块把 Web 层 BlockException 转成 429 JSON；注解 blockHandler 目前返回业务字符串，生产应统一错误码。

## 未覆盖/未验证

- 未启动 Sentinel Dashboard、Nacos 做动态规则实机验证。
- 未做 Cluster 流控、网关流控（Gateway 模块单独覆盖）。
- 未做规则持久化的多环境发布、审计和回滚。
- 未做真实高并发压测，阈值仅作示例。
