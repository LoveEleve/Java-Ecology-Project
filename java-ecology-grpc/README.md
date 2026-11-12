# java-ecology-grpc

gRPC 传输模块：Proto 契约、Unary、Server Streaming、Client Streaming、Bidirectional Streaming，以及 deadline、metadata、retry、health、reflection。

## 定位

覆盖生产 gRPC 的核心使用面：

- `.proto` 契约与代码生成（protobuf-maven-plugin）
- Unary：创建订单
- Server streaming：批量流式返回订单
- Client streaming：批量上传订单
- Bidirectional streaming：聊天式双向流
- Deadline：客户端每次调用设置超时
- Metadata：`x-request-id` 双向透传
- Retry：仅对可重试状态码做有限重试
- Health：标准 gRPC health service
- Reflection：方便 grpcurl/调试
- Keepalive、最大入站消息、优雅停机

不包含：TLS/mTLS、认证鉴权、服务发现/LB、gRPC-Gateway、Observability 自动埋点。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| gRPC Java 1.83.1 | Netty gRPC |
| Protobuf 3.25.5 | 消息与 proto 生成 |
| Spring Boot 3.2.5 | Web 控制面/Actuator |
| grpc-services | health + reflection |
| grpc-inprocess | 单元测试，不占端口 |

## 目录结构

```text
java-ecology-grpc
├─ pom.xml
├─ README.md
├─ grpc-api/                 # order.proto + 生成类
├─ grpc-server/              # Netty gRPC Server，端口 9090
└─ grpc-client/              # Netty gRPC Client，HTTP 控制面 8106
```

## Proto

`grpc-api/src/main/proto/order.proto`：

```proto
service OrderService {
  rpc CreateOrder (CreateOrderRequest) returns (OrderReply);
  rpc StreamOrders (StreamOrdersRequest) returns (stream OrderReply);
  rpc UploadOrders (stream CreateOrderRequest) returns (UploadSummary);
  rpc Chat (stream ChatMessage) returns (stream ChatMessage);
}
```

生成的 Java 包：`com.loveeleve.ecology.grpc.api`。

## 服务端

- 端口：`9090`
- 启动：`GrpcServerLifecycle` 随 Spring 启动
- 服务：`OrderGrpcService`
- Interceptor：`RequestIdServerInterceptor`
- Health：`io.grpc.services.HealthStatusManager`
- Reflection：`ProtoReflectionService`
- 最大入站消息：4 MB
- Keepalive：30s / permit 10s
- 停机：先置 NOT_SERVING，再 graceful shutdown 5s

校验规则：

- `request_id` 非空
- `user_id` / `product_id` 为正
- `quantity > 0`
- `amount` 是正的十进制
- 失败返回 `INVALID_ARGUMENT`

## 客户端

- 目标：`127.0.0.1:9090`
- Deadline：3000 ms
- Retry：UNAVAILABLE，最多 3 次，指数退避 0.1s → 1s
- `x-request-id`：每次调用自动生成并透传
- 支持 unary / server stream / client stream / bidi
- 优雅关闭 channel

HTTP 控制面：

```bash
curl -s -X POST http://localhost:8106/api/grpc/orders \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"grpc-1","userId":1,"productId":2,"amount":"100.00","quantity":1}'
```

响应会转换为普通 JSON，而不是直接暴露 protobuf 内部结构。

## 配置

服务端：

```yaml
app:
  grpc:
    server:
      enabled: true
      port: 9090
      max-inbound-message-size: 4194304
      keep-alive-seconds: 30
      permit-keep-alive-seconds: 10
```

客户端：

```yaml
app:
  grpc:
    client:
      host: 127.0.0.1
      port: 9090
      deadline-millis: 3000
      max-retry-attempts: 3
```

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
./mvnw -f java-ecology-grpc/pom.xml -pl grpc-server spring-boot:run
# 另一个终端
./mvnw -f java-ecology-grpc/pom.xml -pl grpc-client spring-boot:run
```

`grpcurl` 调试：

```bash
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext localhost:9090 ecology.grpc.OrderService/CreateOrder \
  -d '{"request_id":"grpc-1","user_id":"1","product_id":"2","amount":"100.00","quantity":1}'
```

## 测试

```bash
./mvnw -f java-ecology-grpc/pom.xml test
```

当前 9 个测试：

- Unary 返回 CONFIRMED
- 非法请求映射为 `INVALID_ARGUMENT`
- Server streaming 返回指定数量
- Client streaming 汇总 accepted
- Bidirectional streaming 逐条 echo
- 客户端 Unary 调用
- 客户端 Server streaming 聚合
- 真实 Netty gRPC Server 端口收发
- 真实 Netty gRPC Client 配置 + Retry 配置连通

测试同时使用 in-process transport 和本机 ephemeral 端口；不依赖外部服务。

## 生产注意事项

- **proto 字段编号不可随意改**：已发布的字段号/类型必须兼容，新增字段用新编号。
- **deadline 必须设**：没有 deadline 的调用可能永久挂住，拖垮线程/连接。
- **重试只对幂等/可重试错误**：不要对 `INVALID_ARGUMENT` 重试；本模块只重试 UNAVAILABLE。
- **消息上限**：服务端 `maxInboundMessageSize` 和客户端发送大小要匹配，避免 OOM。
- **metadata 是轻量控制信息**：不要把业务大对象塞进 header。
- **流式接口要处理取消/背压**：客户端断开后服务端应停止生产；生产代码要检查 `ServerCallStreamObserver.isReady()`。
- **TLS 必须加**：公网/跨机房用 TLS/mTLS；本模块是 plaintext。
- **认证/限流**：在 Interceptor 层做 token/租户校验和 QPS 限制。
- **LB/服务发现**：gRPC 长连接 + 服务发现需要专门策略，不能直接套 HTTP 轮询。
- **健康/反射要控制暴露面**：reflection 方便调试，但生产只对可信网络开放。

## 未覆盖/未验证

- 未做 TLS/mTLS、认证、限流。
- 未做真实 Netty gRPC Server 端口收发（测试用 in-process）。
- 未做服务发现/LB、gRPC-Gateway、Observability 自动埋点。
- 未做流式接口的取消/背压压测。

## 参考

- [gRPC Java](https://grpc.io/docs/languages/java/)
- [gRPC Spring Boot](https://grpc-ecosystem.github.io/grpc-spring-boot-starter/)
- 本项目根 README 的统一模块约定
