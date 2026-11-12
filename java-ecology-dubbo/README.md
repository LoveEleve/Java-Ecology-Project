# java-ecology-dubbo

Dubbo 3 生产用法模块，按真实服务边界拆分为 **API + Provider + Consumer** 三个 Maven 模块。

## 定位

覆盖研发/生产中的核心场景：

- 独立 `dubbo-api`：接口与 DTO 单独发布，Provider/Consumer 只依赖 API
- 独立 Provider 应用：`@DubboService` 暴露服务
- 独立 Consumer 应用：`@DubboReference` 引用服务
- `version` + `group` 服务隔离
- `timeout` / `retries` / `cluster=failover`
- `loadbalance=roundrobin`
- `GenericService` 泛化调用
- 异步调用
- Attachment 透传
- 参数校验、自定义 Filter

Callback、Mock 和其他高级治理能力暂时不展开，避免把核心 Provider/Consumer 结构做大。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | 两个独立应用 |
| Dubbo 3.3.3 | RPC、注册、泛化、异步 |
| dubbo-registry-nacos | Nacos 注册中心适配 |
| Nacos 2.x | `nacos` profile 使用 |

## 模块结构

```text
java-ecology-dubbo
├─ pom.xml                 聚合 POM
├─ dubbo-api/              接口 + DTO，无 Spring 依赖
│  └─ src/main/java/.../api/OrderService.java
├─ dubbo-provider/         Provider 应用，端口 8090
│  ├─ ProviderApplication.java
│  ├─ provider/OrderServiceImpl.java
│  ├─ filter/EcologyDubboFilter.java
│  └─ resources/application*.yml
├─ dubbo-consumer/         Consumer 应用，端口 8091
│  ├─ ConsumerApplication.java
│  ├─ consumer/OrderConsumer.java
│  ├─ consumer/GenericOrderInvoker.java
│  ├─ controller/DubboController.java
│  └─ resources/application*.yml
└─ README.md
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| API 契约 | `OrderService`、`OrderRequest`、`OrderView` | `dubbo-api/src/main/java/.../api/` | Provider 测试 |
| Provider | `@DubboService OrderServiceImpl` | `dubbo-provider/.../provider/OrderServiceImpl.java` | `DubboProviderTests` |
| Consumer | `@DubboReference OrderConsumer` | `dubbo-consumer/.../consumer/OrderConsumer.java` | Nacos 手工验证 |
| 版本/分组 | `version=1.0.0, group=ecology` | Provider/Consumer 注解 | 配置检查 |
| 超时/重试/容错 | `timeout=3000, retries=2, failover` | 注解 + `application.yml` | 配置检查 |
| 泛化调用 | `GenericOrderInvoker` | `dubbo-consumer/.../consumer/GenericOrderInvoker.java` | Nacos 手工验证 |
| 异步调用 | `OrderService.createAsync` | `dubbo-api`、Provider、Consumer | Provider 测试 + Nacos 手工验证 |
| Attachment | `echoAttachment` | Provider/Consumer | Nacos 手工验证 |
| 参数校验 | `OrderRequest` + `validation=true` | `OrderRequest.java`、Provider 注解 | Provider 测试（合法参数） |
| 自定义 Filter | `EcologyDubboFilter` | `dubbo-provider/.../filter/`、`META-INF/dubbo/...Filter` | 代码/配置 |
| 序列化白名单 | `security/serialize.allowlist` | Provider/Consumer resources | DTO 白名单 |

## 接口清单

Consumer 应用（8091）：

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/dubbo/orders` | 调用 Provider 创建订单 |
| GET | `/api/dubbo/orders/{orderId}` | 调用 Provider 查询订单 |
| GET | `/api/dubbo/ping?message=` | 普通 RPC |
| GET | `/api/dubbo/generic/ping?message=` | 泛化调用 |
| POST | `/api/dubbo/async/orders` | 异步调用 |
| GET | `/api/dubbo/attachment` | Attachment 透传 |
| GET | `/actuator/health` | Consumer 健康检查 |

Provider 应用（8090）仅暴露 Dubbo 服务与 `/actuator/health`。

## 配置

### 默认：N/A 注册中心

Provider：

```yaml
dubbo:
  registry:
    address: N/A
  protocol:
    name: dubbo
    port: 20880
```

Consumer：

```yaml
dubbo:
  registry:
    address: N/A
  consumer:
    check: false
```

Consumer 引用设置 `init=false, lazy=true`，没有注册中心时应用仍可启动；真正调用需要 Provider 可达。

### `nacos` profile

Provider：

```bash
export NACOS_SERVER_ADDR=127.0.0.1:8848
java -jar dubbo-provider/target/dubbo-provider.jar --spring.profiles.active=nacos
```

Consumer：

```bash
java -jar dubbo-consumer/target/dubbo-consumer.jar --spring.profiles.active=nacos
```

Consumer 额外配置：

```yaml
dubbo:
  registry:
    parameters:
      register-consumer-url: true
```

`register-consumer-url=true` 用于泛化 Consumer 在 Nacos 下的服务发现。

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 编译 API + Provider + Consumer
./mvnw -pl :dubbo-provider,:dubbo-consumer -am package

# 先启动 Provider（8090）
java -jar java-ecology-dubbo/dubbo-provider/target/dubbo-provider.jar \
  --spring.profiles.active=nacos

# 再启动 Consumer（8091）
java -jar java-ecology-dubbo/dubbo-consumer/target/dubbo-consumer.jar \
  --spring.profiles.active=nacos
```

## 验证

```bash
# 普通 RPC
curl -s 'http://localhost:8091/api/dubbo/ping?message=split'

# 创建/查询
curl -s -X POST http://localhost:8091/api/dubbo/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":1,"userId":1,"amount":100}'
curl -s http://localhost:8091/api/dubbo/orders/1

# 泛化
curl -s 'http://localhost:8091/api/dubbo/generic/ping?message=split'

# 异步
curl -s -X POST http://localhost:8091/api/dubbo/async/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":2,"userId":1,"amount":200}'

# Attachment
curl -s http://localhost:8091/api/dubbo/attachment
```

Consumer 启动后等待几秒再调用，给 Nacos 服务发现留传播时间。

## 生产注意事项

- **API 包独立版本化**：Provider/Consumer 不能各自复制 DTO 或接口，接口变更要按序列化兼容规则发布。
- **Provider/Consumer 独立部署**：本仓库已经拆分；不要为了演示再把它们塞回一个应用。
- **version/group 是隔离边界**：灰度发布用 version/group 路由，不能只靠服务名。
- **超时和重试要分层**：Consumer timeout、Provider timeout、下游超时要递减；重试要求接口幂等。
- **负载均衡/容错**：`roundrobin + failover` 只是示例；生产按延迟、权重、区域选择，关注重试风暴。
- **泛化调用要限制入口**：泛化入口等于远程方法调用面，必须鉴权、白名单、参数校验。
- **Parameter validation**：Provider 已开启 `validation=true`，需要 `spring-boot-starter-validation`；生产要统一异常映射。
- **Filter 要轻量**：自定义 Filter 在 RPC 线程上执行，不能阻塞或做重 IO。
- **注册中心不是健康检查**：Nacos 有实例不代表业务健康；配合 Actuator/就绪探针/网关摘流。
- **Nacos 鉴权/集群**：本模块只用 standalone 验证，生产要补鉴权、namespace 和集群。

## 未覆盖/未验证

- Callback 暂时移除：Hessian2 下接口类型参数反序列化需要额外白名单/实现，后续单独补。
- Mock 降级示例暂时不启用，避免掩盖真实 Provider 不可用。
- Triple/HTTP2、QoS 未展开。
- 多 Provider 实例负载均衡/故障转移未压测。
- Nacos 集群、鉴权、TLS 未验证。
- Consumer 自动化测试未做；Provider 契约测试已覆盖异步/Attachment。

## 参考

- [Apache Dubbo 官方文档](https://dubbo.apache.org/)
- [Dubbo Spring Boot Starter](https://github.com/apache/dubbo-spring-boot-project)
- 本项目根 README 的统一模块约定
