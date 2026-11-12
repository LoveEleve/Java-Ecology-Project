# java-ecology-netty

Netty TCP 传输模块：Server / Client、长度字段拆包、JSON 协议、心跳与空闲检测、写缓冲背压、断线重连、优雅停机。

## 定位

覆盖生产 TCP 传输的核心问题：

- TCP 粘包/半包：4 字节长度字段 + 最大帧限制
- 编解码：JSON `MessagePacket`，坏帧关闭连接
- 业务 Handler：ECHO / PING / PONG / ERROR
- 空闲检测：`IdleStateHandler`
- 心跳：客户端写空闲发 PING，服务端回 PONG
- 背压：`WRITE_BUFFER_WATER_MARK` + `isWritable()` 检查
- 连接管理：`ChannelGroup`、最大连接数
- 客户端：异步请求/响应、请求超时、断线重连
- 优雅停机：关闭 ServerChannel、ChannelGroup、EventLoopGroup
- 线程模型：boss=1、worker=CPU×2（可配置）

不包含：TLS/mTLS、HTTP/2、WebSocket、Netty native epoll、自定义二进制协议、注册中心、链路追踪接入。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Netty 4.1.115.Final | NIO Server/Client |
| Spring Boot 3.2.5 | Web 控制面、Actuator、配置绑定 |
| Jackson | JSON codec |
| JUnit 5 / EmbeddedChannel | 编解码与 Handler 测试 |

## 目录结构

```text
java-ecology-netty
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/netty
   │  ├─ NettyApplication.java
   │  ├─ codec/MessagePacket.java
   │  ├─ codec/PacketEncoder.java
   │  ├─ codec/PacketDecoder.java
   │  ├─ codec/FrameCodec.java
   │  ├─ handler/ServerMessageHandler.java
   │  ├─ server/NettyServerProperties.java
   │  ├─ server/NettyTcpServer.java
   │  ├─ client/NettyClientProperties.java
   │  ├─ client/ClientMessageHandler.java
   │  ├─ client/NettyTcpClient.java
   │  └─ controller/NettyController.java
   ├─ main/resources/application.yml
   └─ test/java/...（codec、handler、server loopback）
```

## 协议

```text
+----------------+------------------------------+
| 4-byte length  | JSON body (UTF-8)            |
+----------------+------------------------------+
```

`MessagePacket`：

```json
{
  "type": "ECHO",
  "requestId": "req-1",
  "payload": "hello"
}
```

- `type`：`ECHO` / `PING` / `PONG` / `ERROR`
- `requestId`：客户端请求关联 ID
- 最大帧：1 MB（含 4 字节长度头）

## 服务端

| 配置 | 默认 | 说明 |
|---|---|---|
| `app.netty.server.enabled` | true | 随 Spring 启动 |
| `app.netty.server.bind-address` | 0.0.0.0 | 监听地址；生产建议按网络隔离 |
| `app.netty.server.port` | 9000 | TCP 端口 |
| `boss-threads` | 1 | accept 线程 |
| `worker-threads` | 0 | 0 = CPU×2 |
| `max-connections` | 1000 | 超过后关闭新连接 |
| `idle-seconds` | 120 | 读空闲关闭 |
| `write-timeout-seconds` | 10 | 写超时关闭慢连接 |

启动后 Spring HTTP 控制面为 `8104`：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/netty/status` | 运行状态/端口/连接数 |
| POST | `/api/netty/send` | 向第一个可用连接发送 |
| POST | `/api/netty/broadcast` | 广播给所有可写连接 |
| GET | `/actuator/health` | 健康检查 |

`send` 示例：

```json
{"type":"ECHO","requestId":"http-1","payload":"hello"}
```

无连接返回 503；写缓冲满返回 429。

## 客户端

客户端默认关闭，开启后随 Spring 启动并自动重连：

```yaml
app:
  netty:
    client:
      enabled: true
      host: 127.0.0.1
      port: 9000
      reconnect-seconds: 3
      request-timeout-seconds: 5
      connect-timeout-millis: 3000
```

核心 API：

```java
CompletableFuture<MessagePacket> response = client.send(
        new MessagePacket("ECHO", "req-1", "hello"));
```

- `send` 必须提供非空 `requestId`
- 5 秒无响应超时
- 断线后按 `reconnectSeconds × 尝试次数` 退避，最多 30 秒
- 客户端写空闲 30 秒自动 PING

## 配置

```yaml
app:
  netty:
    server:
      enabled: true
      bind-address: 0.0.0.0
      port: 9000
      boss-threads: 1
      worker-threads: 0
      max-connections: 1000
      idle-seconds: 120
      write-timeout-seconds: 10
    client:
      enabled: false
      host: 127.0.0.1
      port: 9000
      reconnect-seconds: 3
      request-timeout-seconds: 5
      connect-timeout-millis: 3000
```

## 运行与测试

```bash
export JAVA_HOME=/env/javaEnv/jdk17
./mvnw -pl java-ecology-netty spring-boot:run
```

```bash
./mvnw -pl java-ecology-netty test
```

当前 12 个测试：

- 编码长度前缀 JSON
- 半包/分片解码
- 超大帧拒绝、非法 JSON 拒绝
- ECHO / PING / ERROR 响应
- 客户端按 requestId 完成请求
- 连接关闭时失败 pending 请求并触发重连回调
- 真实 loopback ServerBootstrap + TCP 客户端收包
- 真实 NettyTcpClient 连接、发送、异步响应和关闭
- Spring 上下文 + 关闭 TCP 时的 status 控制面

测试不依赖外部服务；loopback 只监听本机临时端口。

## 生产注意事项

- **必须限制最大帧**：否则恶意长度字段会打爆内存；本模块 1 MB。
- **必须处理半包/粘包**：不能假设一次 read 就是一条消息。
- **写缓冲要限流**：`isWritable=false` 时不要继续写，否则堆积内存。
- **线程数不要盲目调大**：worker 线程应与 CPU/阻塞模型匹配；业务阻塞要移到独立线程池。
- **心跳/空闲检测要双端配合**：只配一端会出现“假在线”。
- **重连要退避**：固定 1 秒重连会打爆服务端；本模块有上限 30 秒，但仍建议加抖动/最大重试次数。
- **协议要版本化**：生产二进制协议建议加 magic/version/序列化类型；JSON 只适合示例。
- **TLS 要单独加**：公网或跨机房必须 TLS/mTLS，本模块未包含。
- **生产优先 epoll**：Linux 高连接数场景可用 native epoll；本模块用 NIO 保证可移植。
- **没有认证/限流**：TCP 接入层需要在 pipeline 加认证、ACL、连接限流和审计。
- **没有指标**：生产应补连接数、读写字节、积压、错误率、重连次数。

## 未覆盖/未验证

- 未做 TLS/mTLS、认证、连接限流。
- 未做 epoll/native transport、零拷贝、池化调优压测。
- 未做真实公网/跨机房网络抖动、半开连接、海量连接压测。
- 未接入 Micrometer/OpenTelemetry（可复用 observability 模块）。

## 参考

- [Netty User Guide](https://netty.io/wiki/user-guide-for-4.x.html)
- [LengthFieldBasedFrameDecoder](https://netty.io/4.1/api/io/netty/handler/codec/LengthFieldBasedFrameDecoder.html)
- 本项目根 README 的统一模块约定
