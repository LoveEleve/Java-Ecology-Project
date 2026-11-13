# java-ecology-sca-integration

Spring Cloud Alibaba **整体集成**模块：不重复实现单组件，用 Gateway 把已有的 Nacos、LoadBalancer、OpenFeign、Sentinel、Seata 服务串成一条真实请求链路。

## 它解决什么问题

单组件模块各自能跑，但不能证明：

- Nacos 注册的服务名能被 Gateway/Feign 正确发现
- LoadBalancer 在 Gateway 和 Feign 两侧都能选到实例
- Sentinel Gateway 流控不会绕过业务超时/重试
- Seata 全局事务在 Gateway → Order → Account/Storage 链路上仍然成立
- 多组件版本、配置、线程模型能兼容

本模块只做 **Gateway 编排 + 集成约束**，业务服务复用 `java-ecology-seata`，HTTP 下游复用 `java-ecology-http-provider`。

## 请求链路

```text
Client
  ↓
SCA Gateway :8115
  ├─ Nacos Discovery + LoadBalancer
  ├─ Sentinel Gateway
  ↓ /api/sca/orders/**
seata-order-service :8100
  ├─ Nacos Discovery
  ├─ OpenFeign + LoadBalancer
  ├─ Seata @GlobalTransactional
  ↓
seata-account-service :8101 / seata-storage-service :8102
```

HTTP 下游验证：

```text
Client → SCA Gateway → ecology-http-provider :8088
```

## 路由

| 路由 | 下游 | 说明 |
|---|---|---|
| `/api/sca/orders/**` | `lb://seata-order-service` | Rewrite 到 `/api/orders/**` |
| `/api/sca/provider/**` | `lb://ecology-http-provider` | Rewrite 到 `/api/downstream/**` |

- **GET 才重试**：订单创建是 POST，不自动重试，避免重复下单/重复事务。
- **LoadBalancer 不单独做模块**：Gateway 和 OpenFeign 已经覆盖；只有自定义选实例策略时才需要写 `ServiceInstanceListSupplier`/自定义 `LoadBalancer`。

## 配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: ${NACOS_DISCOVERY_ENABLED:false}
        server-addr: ${NACOS_SERVER:127.0.0.1:8848}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
    gateway:
      routes:
        - id: seata-order
          uri: lb://seata-order-service
          predicates:
            - Path=/api/sca/orders/**
          filters:
            - RewritePath=/api/sca/orders(?<segment>/?.*), /api/orders${segment}
```

生产启用：

```bash
export NACOS_DISCOVERY_ENABLED=true
export NACOS_SERVER=nacos-1:8848,nacos-2:8848
export NACOS_GROUP=DEFAULT_GROUP
```

## 启动顺序

本机没有启动这些服务，以下仅为生产/联调顺序：

1. Nacos 集群
2. MySQL 业务库 + `undo_log`
3. Seata TC 集群
4. `seata-account-service`、`seata-storage-service`、`seata-order-service`
5. `java-ecology-http-provider`
6. 本 Gateway

注意：

- 三个 Seata 服务和 Gateway 必须使用同一 Nacos group/namespace。
- `seata-order-service` 的 `NACOS_DISCOVERY_ENABLED=true`。
- Seata TC 的 registry/config 与业务侧一致。
- 订单 POST 不要配置 Gateway Retry。

## 验证

```bash
# 通过 Gateway 创建订单
curl -s -X POST http://localhost:8115/api/sca/orders \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"sca-1","userId":1,"productId":1,"amount":100.00,"quantity":1}'

# 验证 LB + HTTP provider
curl -s http://localhost:8115/api/sca/provider/hello
```

## 测试

```bash
./mvnw -pl java-ecology-sca-integration test
```

当前 1 个测试：

- Nacos/Sentinel 关闭时，Gateway 路由定义仍能加载出 `seata-order` 和 `http-provider`

测试不启动 Nacos、Sentinel、Seata 或业务服务。

## 生产注意事项

- **Gateway 只做接入层**：认证、限流、路由、灰度放这里；业务事务在 Order。
- **Nacos group/namespace 必须统一**：Gateway、Order、Account、Storage 任一不一致都会出现“服务存在但选不到实例”。
- **LoadBalancer 缓存**：生产用 Caffeine，避免每次调用都拉实例列表。
- **Gateway 超时要大于下游 P99**：否则网关先超时，Seata 全局事务可能仍在回滚。
- **不要让 Gateway 重试 POST 订单**：会产生重复全局事务；GET 才重试。
- **Sentinel 资源名**：Gateway 按路由/URL 限流，Order 内部按 Feign/方法限流，两层要分别配。
- **Seata XID 要穿透 Gateway**：本链路只有 Gateway → Order 的 HTTP，XID 由 Order 发起；如果 Gateway 参与事务，需要额外透传。
- **部署顺序**：先 Nacos/TC/DB，再业务服务，最后 Gateway；下线反序。

## 未覆盖/未验证

- 未启动 Nacos/Seata/MySQL/Gateway 做真实端到端验证。
- 未做 Sentinel Gateway 规则、负载均衡压测、跨机房容灾。
- 未做 Gateway 认证和灰度发布。

## 参考

- [Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba)
- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/reference/)
- 本项目根 README 的统一模块约定
