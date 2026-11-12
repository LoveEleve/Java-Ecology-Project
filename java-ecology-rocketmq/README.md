# java-ecology-rocketmq

RocketMQ 生产用法模块：同步/异步/单向/批量/延迟/顺序/事务消息，并发消费、DB 幂等、重试与 DLQ 口径。

## 定位

用 `RocketMQTemplate` + `@RocketMQMessageListener` 覆盖生产常用核心能力：

- 同步、异步（带成功/失败回调）、单向发送
- 批量发送（同一 Topic，受 4 MB 批次上限约束）
- 延迟消息（delay level 1–18）
- 顺序消息（同一 orderId 落同一队列，`ConsumeMode.ORDERLY`）
- 事务消息（本地事务表 + 回查）
- Tag 过滤（`CREATE`）
- 并发消费、消费线程数、最大重试、重试退避
- 消费幂等：`tryClaim → SUCCESS/FAILED` 状态机 + DB 唯一键
- DLQ 口径：超过 `maxReconsumeTimes` 后进入 `%DLQ%consumerGroup`

不包含：RocketMQ 5.x Proxy/gRPC、消息轨迹、ACL/TLS 实机、事务消息的完整业务补偿。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web、Actuator、JDBC |
| RocketMQ 5.1.4 | NameServer/Broker |
| rocketmq-spring-boot-starter 2.3.0 | `RocketMQTemplate`、Listener、TransactionListener |
| H2/MySQL | 消费幂等表和事务状态表 |

## 目录结构

```text
java-ecology-rocketmq
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/rocketmq
   │  ├─ RocketMqApplication.java
   │  ├─ config/RocketMqTopics.java
   │  ├─ dto/OrderEvent.java
   │  ├─ producer/OrderProducer.java
   │  ├─ producer/OrderTransactionListener.java
   │  ├─ consumer/OrderConsumer.java
   │  ├─ consumer/OrderlyOrderConsumer.java
   │  ├─ consumer/TransactionOrderConsumer.java
   │  ├─ service/OrderMessageHandler.java
   │  ├─ service/ProcessedOrderStore.java
   │  ├─ service/TransactionOrderStore.java
   │  └─ controller/RocketMqController.java
   ├─ main/resources/application.yml
   ├─ main/resources/application-mysql.yml
   ├─ main/resources/db/schema-h2.sql
   ├─ main/resources/db/schema-mysql.sql
   └─ test/java/...（幂等/事务/Producer 单测）
```

## Topic 设计

| 常量 | Topic | 用途 | 消费模式 |
|---|---|---|---|
| `ORDERS` | `ecology-rocketmq-orders` | 普通/异步/单向/批量/延迟 | 并发 |
| `ORDERED` | `ecology-rocketmq-ordered` | 顺序消息 | 顺序 |
| `TRANSACTION` | `ecology-rocketmq-transaction` | 事务消息提交后投递 | 并发 |

## 生产者核心用法

| 能力 | API | 关键点 |
|---|---|---|
| 同步发送 | `syncSend` | 等 Broker ACK；失败抛异常，可重试 |
| 异步发送 | `asyncSend` + `SendCallback` | 回调记录 msgId/异常，不阻塞调用线程 |
| 单向发送 | `sendOneWay` | 不等 ACK，可能丢消息，只适合日志/埋点 |
| 批量发送 | `sendBatch` | 同一 Topic、同一批次总大小受限 |
| 延迟消息 | `sendDelay(level)` | level 1–18；5.x 也可用延迟时间 |
| 顺序消息 | `sendOrdered` | 同一 `orderId` 作 hashKey，保证同队列有序 |
| 事务消息 | `sendTransaction` | 先发半消息，本地事务提交后可见 |

生产者参数：

| 参数 | 本模块值 | 说明 |
|---|---|---|
| `rocketmq.producer.group` | `ecology-rocketmq-producer` | 事务回查依赖该 group，必须稳定 |
| `send-message-timeout` | 3000 ms | 同步/批量发送超时 |
| `retry-times-when-send-failed` | 3 | 同步发送失败重试 |
| `retry-times-when-send-async-failed` | 3 | 异步发送失败重试 |
| `retry-next-server` | true | 失败换 Broker 重试 |
| `compress-message-body-threshold` | 4096 | 超过阈值压缩 |
| `max-message-size` | 4194304 | 与 Broker 限制保持一致 |
| `tls-enable` / `namespace` | 环境变量 | TLS 与命名空间 |
| `access-key` / `secret-key` | 环境变量 | ACL；不要硬编码 |

消息头统一设置：

- `KEYS = orderId`：用于业务查询、事务回查、幂等定位
- `TAGS = CREATE`：与消费者 `selectorExpression = "CREATE"` 对齐

## 消费者核心用法

| 能力 | 实现 | 关键点 |
|---|---|---|
| 并发消费 | `OrderConsumer` | `ConsumeMode.CONCURRENTLY`，线程数受队列数限制 |
| 顺序消费 | `OrderlyOrderConsumer` | `ConsumeMode.ORDERLY`，失败挂起当前队列 |
| 事务消息消费 | `TransactionOrderConsumer` | 独立 group，消费 `TRANSACTION` topic |
| Tag 过滤 | `selectorExpression = "CREATE"` | 生产端 tag 必须一致 |
| 异常重试 | 直接抛异常 | 框架返回 `RECONSUME_LATER` / 挂起队列 |
| 幂等 | `ProcessedOrderStore.tryClaim` | DB 唯一键 + 状态机，不依赖内存 |
| DLQ | `maxReconsumeTimes = 3` | 超限进入 `%DLQ%consumerGroup` |

消费者参数（注解是权威配置）：

| 参数 | 并发消费者 | 顺序消费者 | 说明 |
|---|---|---|---|
| `consumeThreadMax` | 32 | 8 | 消费线程上限 |
| `consumeThreadNumber` | 20 | 8 | 初始线程数 |
| `maxReconsumeTimes` | 3 | 3 | 最大重试次数 |
| `delayLevelWhenNextConsume` | 1 | — | 并发重试退避级别 |
| `suspendCurrentQueueTimeMillis` | — | 1000 ms | 顺序消费失败挂起时间 |
| `messageModel` | CLUSTERING | CLUSTERING | 集群消费；广播模式不保证重试/DLQ |
| `awaitTerminationMillisWhenShutdown` | 5000 ms | 5000 ms | 优雅停机等待 |

`rocketmq.consumer.pull-batch-size=32` 控制单次拉取批量；它不是消费线程数。

## 幂等状态机

`rocketmq_processed_order`：

| 字段 | 说明 |
|---|---|
| `order_id` | 唯一键 |
| `status` | `PROCESSING` / `SUCCESS` / `FAILED` |
| `processed_at` | 成功时间 |
| `updated_at` | 状态更新时间 |

流程：

1. `tryClaim` 插入 `PROCESSING`；重复消息返回 `false`。
2. 业务处理成功 → `markSuccess`。
3. 业务异常 → `markFailed` 后抛出，触发 RocketMQ 重试。
4. 重试消息看到 `FAILED` 可重新 claim；看到 `SUCCESS` 直接跳过；看到未过期的 `PROCESSING` 抛异常触发重试，避免把正在处理的消息误 ACK。
5. `PROCESSING` 超过 30 秒 lease 后可被回收，防止消费者崩溃后消息永久卡死。

注意：lease 回收仍可能造成重复副作用，关键业务必须使用本地事务/Outbox；仅靠唯一键不是完整幂等。

## 事务消息

`OrderTransactionListener`：

- `executeLocalTransaction`：写 `rocketmq_transaction_order` 为 `COMMITTED`，返回 `COMMIT`
- 本地事务失败：写 `ROLLBACK`，返回 `ROLLBACK`
- `checkLocalTransaction`：按 `KEYS` 查事务表，返回 `COMMIT` / `ROLLBACK` / `UNKNOWN`

这是可运行的结构示例，不代表真实业务补偿；生产应把本地业务和事务状态放在同一个数据库事务里。

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/rocketmq/orders` | 同步发送 |
| POST | `/api/rocketmq/orders/batch` | 批量发送 |
| POST | `/api/rocketmq/orders/async` | 异步发送 |
| POST | `/api/rocketmq/orders/oneway` | 单向发送 |
| POST | `/api/rocketmq/orders/delay?level=3` | 延迟消息 |
| POST | `/api/rocketmq/orders/ordered` | 顺序消息 |
| POST | `/api/rocketmq/orders/transaction` | 事务消息 |
| GET | `/api/rocketmq/stats` | 已成功处理数量 |
| GET | `/actuator/health` | 健康检查 |

## 配置

```yaml
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:127.0.0.1:9876}
  producer:
    group: ecology-rocketmq-producer
    send-message-timeout: 3000
    retry-times-when-send-failed: 3
    retry-times-when-send-async-failed: 3
    retry-next-server: true
    tls-enable: ${ROCKETMQ_TLS_ENABLE:false}
    namespace: ${ROCKETMQ_NAMESPACE:}
    compress-message-body-threshold: 4096
    max-message-size: 4194304
  consumer:
    namespace: ${ROCKETMQ_NAMESPACE:}
    tls-enable: ${ROCKETMQ_TLS_ENABLE:false}
    pull-batch-size: 32
app:
  rocketmq:
    consumer-group: ${ROCKETMQ_CONSUMER_GROUP:ecology-rocketmq-consumer}
    ordered-group: ${ROCKETMQ_ORDERED_GROUP:ecology-rocketmq-ordered-consumer}
    transaction-group: ${ROCKETMQ_TRANSACTION_GROUP:ecology-rocketmq-transaction-consumer}
```

生产 NameServer 用分号分隔多个地址，并配置 ACL 的 `ROCKETMQ_ACCESS_KEY` / `ROCKETMQ_SECRET_KEY`。

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
export ROCKETMQ_NAME_SERVER=127.0.0.1:9876
./mvnw -pl java-ecology-rocketmq spring-boot:run
```

需要 NameServer + Broker，并提前创建三个 Topic（或允许 autoCreateTopic）。

## 验证

本机没有启动 RocketMQ Broker，自动化只覆盖可离线验证的部分：

```bash
./mvnw -pl java-ecology-rocketmq test
```

当前 12 个单测覆盖：

- `ProcessedOrderStore`：重复 claim、处理中重试、过期 claim 回收、失败重试
- `TransactionOrderStore`：事务状态写入/读取
- `OrderProducer`：Topic/tag/keys、顺序 Topic、事务 Topic、延迟级别校验
- `OrderTransactionListener`：本地事务提交、回查 `COMMIT`/`UNKNOWN`

真实收发需要 Broker：

```bash
curl -s -X POST http://localhost:8098/api/rocketmq/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"rmq-1","userId":"user-1","amount":100,"fail":false}'

curl -s -X POST http://localhost:8098/api/rocketmq/orders/ordered \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"rmq-order-1","userId":"user-1","amount":100,"fail":false}'

curl -s http://localhost:8098/api/rocketmq/stats
```

## 生产注意事项

- **消费幂等落库**：RocketMQ 是 At-Least-Once；内存 Set 或先查后写都有并发窗口。
- **重试有上限**：超过 `maxReconsumeTimes` 进 DLQ；DLQ 必须监控、修复、重放。
- **顺序只到队列级**：同一 hashKey 保证同队列；不同 orderId 之间不保证顺序。
- **并发线程数受队列数限制**：Topic 队列不足时，调大 `consumeThreadMax` 不会提升吞吐。
- **事务消息不是分布式事务银弹**：本地事务、回查、消费者幂等三件事都要成立。
- **Producer group 稳定**：事务消息回查依赖 group，不能按实例动态命名。
- **Tag 过滤不是安全边界**：只是减少拉取，消费端仍要校验消息。
- **单向/异步不保证可靠投递**：关键消息用同步发送 + 业务补偿。
- **批量消息同 Topic 且受大小限制**：控制单批数量，避免超过 Broker `max-message-size`。
- **NameServer/Broker 按集群设计**：多 NameServer、多主多从、Topic 多副本；不把单机当生产基线。

## 未覆盖/未验证

- 未启动真实 NameServer/Broker 做收发验证。
- 未做事务消息回查失败/回滚实战。
- 未做 DLQ 重放工具、消息轨迹、ACL/TLS 实机。
- 未接入 RocketMQ 5.x Proxy/gRPC。

## 参考

- [RocketMQ 官方文档](https://rocketmq.apache.org/docs/)
- [rocketmq-spring](https://github.com/apache/rocketmq-spring)
- 本项目根 README 的统一模块约定
