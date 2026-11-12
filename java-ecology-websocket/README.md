# java-ecology-websocket

WebSocket 传输模块：Raw JSON WebSocket、握手鉴权、会话注册、广播/私聊、心跳、背压、连接/消息/空闲限制。

## 定位

覆盖生产 WebSocket 的核心问题：

- 握手鉴权：Bearer token 或 query token
- Origin 白名单
- 会话注册：userId → sessions
- 广播、按用户私聊
- 文本消息大小限制
- 空闲超时
- 服务端心跳 PING
- 发送背压：`ConcurrentWebSocketSessionDecorator`
- 最大连接数
- 优雅停机

不包含：STOMP、SockJS、Redis 跨实例广播、TLS/WSS、JWT 签发/刷新、消息持久化。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | WebSocket、Web、Actuator |
| Spring WebSocket | `TextWebSocketHandler`、`HandshakeInterceptor` |
| Tomcat WebSocket | 容器级 buffer/idle 配置 |
| JUnit 5 + JDK HttpClient | 真实 WebSocket 集成测试 |

## 目录结构

```text
java-ecology-websocket
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/websocket
   │  ├─ WebSocketApplication.java
   │  ├─ config/WebSocketConfig.java
   │  ├─ config/WebSocketProperties.java
   │  ├─ interceptor/WebSocketAuthInterceptor.java
   │  ├─ dto/WsMessage.java
   │  ├─ handler/ChatWebSocketHandler.java
   │  ├─ service/WebSocketSessionRegistry.java
   │  └─ controller/WebSocketController.java
   ├─ main/resources/application.yml
   └─ test/java/...（握手鉴权 + 真实 WebSocket 收发）
```

## 连接

```text
ws://localhost:8107/ws/chat?token=dev-token&userId=u1
```

也支持：

```text
Authorization: Bearer dev-token
X-User-Id: u1
```

- token 错误/缺失：握手返回 401
- `Origin` 必须在 `allowed-origins` 中
- 达到 `max-sessions` 后新连接关闭

## 消息协议

```json
{
  "type": "CHAT",
  "from": "u1",
  "to": "u2",
  "payload": "hello",
  "timestamp": 1730000000000
}
```

| type | 方向 | 说明 |
|---|---|---|
| `WELCOME` | server → client | 连接成功 |
| `CHAT` | 双向 | `to` 为空广播，否则私聊 |
| `PING` / `PONG` | 双向 | 心跳/探活 |
| `ERROR` | server → client | 非法消息/未知类型 |

服务端每 25 秒广播 `PING`；客户端可回 `PING` 或直接使用业务消息维持活跃。

## HTTP 控制面

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/ws/status` | 连接数/在线用户 |
| POST | `/api/ws/broadcast` | 广播 |
| POST | `/api/ws/send?userId=u2` | 私聊 |
| GET | `/actuator/health` | 健康检查 |

## 配置

```yaml
app:
  websocket:
    path: /ws/chat
    auth-token: ${WS_AUTH_TOKEN:dev-token}
    allowed-origins: ${WS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8107}
    max-sessions: 1000
    max-text-message-bytes: 65536
    idle-timeout-millis: 300000
    send-time-limit-millis: 5000
    send-buffer-size-bytes: 524288
    heartbeat-millis: 25000
```

## 运行与测试

```bash
export JAVA_HOME=/env/javaEnv/jdk17
./mvnw -pl java-ecology-websocket spring-boot:run
```

```bash
./mvnw -pl java-ecology-websocket test
```

当前 6 个测试：

- 握手 query token 校验 + userId 提取
- Bearer header token 校验
- 错误 token 返回 401
- 错误 Origin 握手拒绝
- 真实 Tomcat 连接、WELCOME、CHAT、PING/PONG/heartbeat
- 私聊只到达目标用户，且 from 使用认证身份

测试启动随机端口，不使用外部服务。

## 生产注意事项

- **WSS 必须由 TLS 终止层提供**：生产不能直接暴露 `ws://`。
- **token 不要放 URL**：query token 容易进日志/代理；优先 Authorization/Cookie/短期 ticket。默认 `dev-token` 只能用于本地，生产必须换。
- **Origin 白名单**：防止跨站 WebSocket 劫持；不要用 `*`。
- **多实例广播**：单机 registry 只能广播本机连接；集群需要 Redis Pub/Sub、Kafka 或专用网关。
- **负载均衡**：长连接需要足够的 idle timeout、连接数上限和优雅摘流；广播场景考虑 sticky session。
- **心跳**：代理/LB 的空闲断开时间要小于心跳间隔，否则会出现“在线但收不到”。
- **背压**：`ConcurrentWebSocketSessionDecorator` 超时会关闭慢客户端；业务不要无限 `sendMessage`。
- **消息大小**：容器 buffer 只是第一层，业务层还要限制 payload 和反序列化深度。
- **REST 控制面要保护**：`/api/ws/broadcast`、`/send` 不能公网无鉴权开放。
- **没有 STOMP/SockJS**：浏览器兼容性和订阅模型需要时再引入。

## 未覆盖/未验证

- 未做 WSS/TLS、JWT、Redis 跨实例广播、STOMP/SockJS。
- 未做高并发连接、慢客户端、断网重连压测。
- 未做消息持久化、离线消息、ACK/重投。

## 参考

- [Spring WebSocket](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [Tomcat WebSocket](https://tomcat.apache.org/tomcat-10.1-doc/web-socket-howto.html)
- 本项目根 README 的统一模块约定
