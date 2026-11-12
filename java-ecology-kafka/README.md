# java-ecology-kafka

Spring Kafka 生产用法模块：幂等 Producer、手动 ack、并发消费、消费幂等、重试和死信 Topic。

## 定位

覆盖研发/生产中的核心场景：

- Producer：`acks=all`、幂等、重试、压缩、linger/batch、key 路由
- Consumer：关闭自动提交、`MANUAL_IMMEDIATE`、并发消费
- Batch listener：一次 poll 批量交给业务，提升吞吐
- 消费幂等：数据库唯一键；H2 本地、MySQL 生产 profile
- 异常重试：`DefaultErrorHandler` + `FixedBackOff`
- DLT：`DeadLetterPublishingRecoverer`，独立 DLT 消费者
- Topic：分区、副本、保留时间；本地副本 1，生产副本 3
- Embedded Kafka 测试，不依赖外部 Kafka

不包含：Schema Registry/Avro、Kafka Streams、跨集群复制、Exactly-Once 端到端事务。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web、Actuator |
| Spring Kafka 3.1.4 | Producer/Consumer/ErrorHandler |
| Kafka 3.6.2 | 消息队列 |
| Embedded Kafka | 自动化测试 |
| JSON | `JsonSerializer`/`JsonDeserializer` |

## 目录结构

```text
java-ecology-kafka
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/kafka
   │  ├─ KafkaApplication.java
   │  ├─ config/KafkaConfig.java
   │  ├─ config/KafkaTopics.java
   │  ├─ producer/OrderProducer.java
   │  ├─ consumer/OrderConsumer.java
   │  ├─ consumer/OrderDltConsumer.java
   │  ├─ service/ProcessedOrderStore.java
   │  ├─ controller/KafkaController.java
   │  └─ dto/OrderEvent.java
   ├─ main/resources/application.yml
   └─ test/java/.../KafkaIntegrationTests.java
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| Producer | `OrderProducer` | `producer/OrderProducer.java` | `KafkaIntegrationTests` |
| 幂等/acks/重试/压缩 | YAML | `application.yml` | 配置/测试 |
| 并发消费 | `@KafkaListener` + listener concurrency | `consumer/OrderConsumer.java`、`application.yml` | `KafkaIntegrationTests` |
| 手动 ack | `Acknowledgment` | `consumer/OrderConsumer.java` | `KafkaIntegrationTests` |
| Batch listener | `BatchOrderConsumer` + batch factory | `consumer/BatchOrderConsumer.java`、`config/BatchKafkaConfig.java` | `KafkaIntegrationTests` |
| 消费幂等 | `ProcessedOrderStore` + DB 唯一键 | `service/ProcessedOrderStore.java`、`db/schema-*.sql` | `KafkaIntegrationTests` |
| 异常重试 | `DefaultErrorHandler` | `config/KafkaConfig.java` | DLT 测试 |
| Poison message 反序列化 | `ErrorHandlingDeserializer` | `application.yml` | 配置 |
| DLT | `DeadLetterPublishingRecoverer` / `OrderDltConsumer` | `config/KafkaConfig.java`、`consumer/OrderDltConsumer.java` | `KafkaIntegrationTests` |
| Topic 创建 | `NewTopic` | `config/KafkaConfig.java` | Embedded Kafka |
| 核心参数 | producer/consumer YAML | `application.yml` | 配置/测试 |
| HTTP 入口 | `KafkaController` | `controller/KafkaController.java` | 本机/接口 |

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/kafka/orders` | 发送订单事件 |
| GET | `/api/kafka/stats` | 已处理/失败计数 |
| GET | `/actuator/health` | 健康检查 |

## 配置

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}
    producer:
      acks: all
      retries: 2147483647
      compression-type: lz4
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
        linger.ms: 10
        batch.size: 32768
    consumer:
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
      enable-auto-commit: false
      auto-offset-reset: earliest
      max-poll-records: 100
    listener:
      ack-mode: manual_immediate
      concurrency: 3
```

生产 Topic 副本数应 ≥3，`min.insync.replicas=2`；本模块本地为了 Embedded Kafka 使用副本 1。

## 核心参数

### Producer

| 参数 | 值 | 说明 |
|---|---|---|
| `acks` | `all` | 所有 ISR 确认 |
| `enable.idempotence` | `true` | 防 Broker 重试重复 |
| `retries` | `2147483647` | 配合幂等重试 |
| `max.in.flight.requests.per.connection` | `5` | 幂等允许的最大在途 |
| `compression.type` | `lz4` | 压缩 |
| `linger.ms` / `batch.size` | `10` / `32768` | 批量发送 |
| `delivery.timeout.ms` | `120000` | 端到端发送上限 |
| `request.timeout.ms` | `30000` | 单请求超时 |
| `max.block.ms` | `60000` | metadata/buffer 阻塞上限 |

### Consumer

| 参数 | 值 | 说明 |
|---|---|---|
| `enable-auto-commit` | `false` | 手动提交 |
| `ack-mode` | `manual_immediate` | 业务成功后 ack |
| `concurrency` | `3` | 并发消费者数，受分区数限制 |
| `max-poll-records` | `100` | 单次 poll 上限 |
| `max.poll.interval.ms` | `300000` | 处理超时触发 rebalance |
| `session.timeout.ms` / `heartbeat.interval.ms` | `45000` / `3000` | 会话与心跳 |
| `fetch.min.bytes` / `fetch.max.bytes` | `1` / `50MB` | 拉取策略 |
| `auto-offset-reset` | `earliest` | 无 offset 时从最早开始 |
| `ErrorHandlingDeserializer` | 启用 | 反序列化失败不阻塞 |

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
export KAFKA_BOOTSTRAP_SERVERS=127.0.0.1:9092
./mvnw -pl java-ecology-kafka spring-boot:run
```

## 验证

```bash
curl -s -X POST http://localhost:8097/api/kafka/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"order-1","userId":"user-1","amount":100,"fail":false}'

curl -s http://localhost:8097/api/kafka/stats
```

测试使用 Embedded Kafka，自动创建 `ecology-orders`、`ecology-orders.DLT`、`ecology-orders-batch`，覆盖单条消费、批量消费、重复消息（DB 唯一键）和失败进 DLT。

## 生产注意事项

- **Producer 幂等不等于业务幂等**：`enable.idempotence=true` 只防 Broker 重试重复，业务消费仍要唯一键/去重表。
- **acks=all + min.insync.replicas**：生产至少 3 副本、ISR 2，才能承受单 Broker 故障。
- **手动 ack 必须在业务成功后提交**：先 ack 再处理会丢消息；处理失败不要吞异常。
- **重试要有上限**：无限重试会阻塞分区；设置 `DefaultErrorHandler` backoff 和 DLT。
- **DLT 也要消费和告警**：DLT 不是垃圾桶，必须监控、修复、重放。
- **并发数不是越大越好**：受分区数限制；同 key 消息在单分区内有序，跨分区不保证顺序。
- **消费幂等生产实现**：本模块用数据库唯一键；多实例消费同一分区组时由 DB 唯一约束兜底，Redis SETNX 也可。
- **不要用 Kafka 当数据库**：消息保留期有限，查询/对账走业务库或数仓。
- **Schema 管理**：生产建议 Avro/Protobuf + Schema Registry，JSON 只适合轻量事件。
- **Exactly-Once**：需要事务 Producer + 消费端事务/幂等，本模块未展开。

## 未覆盖/未验证

- 未接真实 Kafka 集群、Schema Registry、ACL/SASL/TLS。
- 未做端到端 Exactly-Once 事务。
- 未做 Kafka Streams、跨集群复制、分区扩容演练。
- Batch listener 未做单批失败/部分成功的专项测试。
- H2 只用于本地/测试；生产 `mysql` profile 的 DB 幂等表需迁移工具管理。
- Poison message 的 DLT 处理只做配置，未做畸形 JSON 专项测试。

## 参考

- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/)
- [Apache Kafka 官方文档](https://kafka.apache.org/documentation/)
- 本项目根 README 的统一模块约定
