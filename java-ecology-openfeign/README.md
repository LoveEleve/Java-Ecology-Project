# java-ecology-openfeign

Spring Cloud OpenFeign 生产用法模块：真实 Nacos 服务发现 + LoadBalancer + Sentinel 熔断/限流/降级 + HC5 连接池 + 超时/重试。

## 定位

Consumer 只依赖服务名，不写死下游 URL：

- Nacos 注册中心发现 `ecology-http-provider`
- Spring Cloud LoadBalancer 选择实例
- Sentinel 负责流控、熔断、降级
- OpenFeign HC5 连接池负责连接复用和并发上限
- Feign 超时、重试、拦截器、错误解码、日志

独立 Provider 见 `java-ecology-http-provider`。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web、Actuator |
| Spring Cloud 2023.0.3 | OpenFeign、LoadBalancer |
| Spring Cloud Alibaba 2023.0.1.2 | Nacos Discovery、Sentinel |
| OpenFeign HC5 | Apache HttpClient 5 连接池 |
| Nacos 2.x | 注册中心 |
| Sentinel | 流控/熔断/降级 |

## 目录结构

```text
java-ecology-openfeign
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/openfeign
   │  ├─ OpenFeignApplication.java
   │  ├─ client/DownstreamClient.java
   │  ├─ client/DownstreamClientFallbackFactory.java
   │  ├─ service/DownstreamService.java
   │  ├─ config/DemoFeignConfig.java
   │  ├─ config/SentinelRulesConfig.java
   │  ├─ controller/FeignController.java
   │  └─ dto/EchoResponse.java
   ├─ main/resources/application.yml
   └─ test/java/.../FallbackFactoryTests.java
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| Feign 服务发现 | `@FeignClient(name=...)` | `client/DownstreamClient.java` | 代码/配置 |
| LoadBalancer | Nacos + LoadBalancer | `pom.xml`、`application.yml` | 代码/配置 |
| Sentinel 流控 | `SentinelRulesConfig` | `config/SentinelRulesConfig.java` | 代码/配置 |
| Sentinel 熔断 | `DegradeRule` | `config/SentinelRulesConfig.java` | 代码/配置 |
| Feign 降级 | `fallbackFactory` | `client/DownstreamClientFallbackFactory.java` | `FallbackFactoryTests` |
| 方法级降级 | `@SentinelResource` | `service/DownstreamService.java` | 代码/配置 |
| HC5 连接池 | `feign.httpclient.hc5` | `application.yml`、`pom.xml` | 编译/配置 |
| 超时/重试 | `Request.Options` / `Retryer` | `config/DemoFeignConfig.java` | 代码/配置 |
| 请求头/错误解码/日志 | `RequestInterceptor` / `ErrorDecoder` / `Logger.Level` | `config/DemoFeignConfig.java` | 代码 |

## 接口清单

Consumer `8094`：

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/feign/hello?name=` | Feign -> Nacos -> Provider |
| GET | `/api/feign/echo?message=` | 透传 traceId |
| GET | `/api/feign/not-found` | 404 错误解码 |
| GET | `/api/feign/slow?millis=` | 触发超时/熔断/降级 |
| GET | `/actuator/health` | 健康检查 |

## 关键配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        group: ECO_ECOLOGY
    openfeign:
      client:
        config:
          default:
            connectTimeout: 1000
            readTimeout: 3000
      httpclient:
        hc5:
          enabled: true
        max-connections: 200
        max-connections-per-route: 50
        time-to-live: 900
        time-to-live-unit: seconds
feign:
  sentinel:
    enabled: true
```

Sentinel 规则代码在 `SentinelRulesConfig`：`downstream.hello` QPS 20、RT 1000ms 熔断；`downstream.echo` 线程数 20 并发限制。

生产动态规则使用 `--spring.profiles.active=sentinel-nacos`：规则从 Nacos 的 `ECO_ECOLOGY_SENTINEL` group 加载，并关闭本地规则；dataId 为 `java-ecology-openfeign-flow-rules` / `java-ecology-openfeign-degrade-rules`。

连接池/并发补充：Caffeine 缓存 LoadBalancer 实例、HC5 maxConnections 200/perRoute 50、Feign 请求/响应压缩、Nacos username/password 环境变量。

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
export NACOS_SERVER_ADDR=127.0.0.1:8848

# Terminal 1
java -jar java-ecology-http-provider/target/java-ecology-http-provider.jar

# Terminal 2
java -jar java-ecology-openfeign/target/java-ecology-openfeign.jar
```

## 验证

```bash
curl -s 'http://localhost:8094/api/feign/hello?name=nacos'
curl -s 'http://localhost:8094/api/feign/echo?message=hello'
curl -s 'http://localhost:8094/api/feign/slow?millis=5000'
```

本机已用 Nacos 2.3.2 + Provider + Consumer 手工验证：hello、echo、慢调用超时 fallback 均通过；自动化测试只测 Provider 上下文和 FallbackFactory，不启动 Nacos。

## 生产注意

- **没有 URL 直连**：Consumer 只用服务名，Nacos + LoadBalancer 负责实例选择。
- **超时分层**：connect 1s、read 3s、Sentinel 降级窗口 10s；上游接口超时要大于下游。
- **重试与熔断要一起看**：重试会放大流量，Sentinel 熔断打开后快速失败。
- **HC5 连接池**：`max-connections` 和 `max-connections-per-route` 要按下游实例数和并发估算；连接池不是越大越好。
- **Sentinel 规则不要只写死在代码**：生产用 Dashboard/配置中心动态规则、持久化到 Nacos。
- **Fallback 要返回可识别结果**：不要让降级静默变成成功；记录指标、告警和 traceId。
- **Provider 慢接口要隔离**：线程池/超时/熔断，避免拖垮 Consumer。
- **Nacos 鉴权/集群**：standalone 配置不能直接生产。

## 未覆盖/未验证

- 未启动 Sentinel Dashboard 做动态规则/可视化验证。
- 未做多 Provider 实例负载均衡/故障转移压测。
- `sentinel-nacos` profile 已给配置，但未启动 Nacos 配置中心验证规则推送。
- 未做 Gateway 层限流/熔断。

## 参考

- [Spring Cloud OpenFeign](https://docs.spring.io/spring-cloud-openfeign/reference/)
- [Spring Cloud Alibaba Sentinel](https://github.com/alibaba/spring-cloud-alibaba/wiki/Sentinel)
- [Sentinel 官方文档](https://sentinelguard.io/)
- 本项目根 README 的统一模块约定
