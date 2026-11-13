# java-ecology-canal

Canal CDC 模块：订阅 MySQL binlog、批量 getWithoutAck、RowChange 解析、ack/rollback、断线重连和运行状态。

## 定位

覆盖 Canal 客户端生产使用的主链路：

- Single / Cluster（ZooKeeper）连接
- destination + filter 订阅
- 批量 `getWithoutAck`
- INSERT / UPDATE / DELETE / DDL 解析
- 处理成功后 `ack(batchId)`
- 处理失败 `rollback(batchId)`
- 异常断线、退避重连
- 优雅停机
- 运行状态/处理量/最后 batch/错误查询
- 可替换的 `CanalEventHandler` 下游处理入口

不包含：Canal Server、MySQL binlog 配置、下游 MQ/ES 写入、Exactly-Once、Canal Admin、HA 选主。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | 生命周期、Web、Actuator |
| canal.client 1.1.7 | Canal 客户端 |
| canal.protocol | CanalEntry/RowChange |
| JUnit 5 | protobuf 解析测试 |

## 目录结构

```text
java-ecology-canal
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/canal
   │  ├─ CanalApplication.java
   │  ├─ config/CanalProperties.java
   │  ├─ controller/CanalController.java
   │  ├─ handler/CanalEventHandler.java
   │  ├─ model/CanalRowEvent.java
   │  ├─ service/CanalClient.java
   │  └─ service/CanalMessageParser.java
   ├─ main/resources/application.yml
   └─ test/java/...（INSERT/UPDATE/DDL 解析 + 上下文）
```

## 数据流

```text
MySQL binlog
   ↓
Canal Server (canal.instance)
   ↓  subscribe(destination, filter)
Canal Client
   ↓  getWithoutAck(batchSize)
CanalMessageParser
   ↓  List<CanalRowEvent>
CanalEventHandler
   ↓ success: ack(batchId)
   ↓ failure: rollback(batchId)
```

## 配置

```yaml
app:
  canal:
    enabled: ${CANAL_ENABLED:false}
    destination: ${CANAL_DESTINATION:example}
    host: ${CANAL_HOST:127.0.0.1}
    port: ${CANAL_PORT:11111}
    username: ${CANAL_USERNAME:canal}
    password: ${CANAL_PASSWORD:canal}
    cluster-enabled: ${CANAL_CLUSTER_ENABLED:false}
    zookeeper-servers: ${CANAL_ZOOKEEPER_SERVERS:127.0.0.1:2181}
    filter: ${CANAL_FILTER:.*\..*}
    batch-size: 1000
    get-timeout-millis: 1000
    reconnect-backoff-millis: 3000
```

生产建议：

- `enabled=true` 显式开启
- MySQL `binlog_format=ROW`、`binlog_row_image=FULL`
- destination 与 Canal Server instance 一致
- filter 按库表收窄，不要全库全表
- Cluster 模式使用 ZooKeeper 做 HA
- 连接凭据走密钥管理，不落盘

## 状态接口

`GET /api/canal/status`

```json
{
  "enabled": true,
  "running": true,
  "destination": "example",
  "filter": "demo\\..*",
  "processedEvents": 12,
  "lastBatchId": 100,
  "lastError": ""
}
```

## 解析模型

`CanalRowEvent`：

```java
record CanalRowEvent(
    String destination,
    String database,
    String table,
    String eventType,
    Map<String, String> before,
    Map<String, String> after,
    long executeTime,
    long batchId,
    long entryId
)
```

- `INSERT`：after 有值
- `UPDATE`：before/after 都有值
- `DELETE`：before 有值
- `DDL`：after 里放 `sql`

`entryId` 使用 binlog offset；同一条 SQL 的定位以 `batchId + offset` 为准。

## 运行与测试

```bash
export JAVA_HOME=/env/javaEnv/jdk17
export CANAL_ENABLED=true
./mvnw -pl java-ecology-canal spring-boot:run
```

```bash
./mvnw -pl java-ecology-canal test
```

当前 5 个测试：

- INSERT after 列解析
- UPDATE before/after 解析
- DDL sql 解析
- Parser → Sink 事件投递链路
- Spring 上下文（Canal 关闭，不连接外部）

没有 Canal Server 时不会启动消费者线程；测试不依赖外部服务。


## 投递 Kafka / RocketMQ / Elasticsearch

Canal 有三种投递方式，**不要混用**：

### 1. MQ 原生模式（推荐用于 Kafka/RocketMQ）

由 **Canal Server 直接写 MQ**，不需要本模块的 TCP Client：

`canal.properties`：

```properties
canal.serverMode = kafka
canal.mq.flatMessage = true
canal.mq.canalBatchSize = 50
canal.mq.canalGetTimeout = 100

kafka.bootstrap.servers = kafka-1:9092,kafka-2:9092
kafka.acks = all
kafka.compression.type = lz4
kafka.max.in.flight.requests.per.connection = 1
```

实例 `instance.properties`：

```properties
canal.mq.topic=ecology-cdc
canal.mq.partition=0
```

RocketMQ 同理：

```properties
canal.serverMode = rocketMQ
rocketmq.namesrv.addr = rmq-1:9876;rmq-2:9876
rocketmq.producer.group = canal-cdc-producer
canal.mq.topic = ecology-cdc
```

此时业务侧用 Kafka/RocketMQ 消费者消费 JSON flat message，不再启动本模块的 `CanalClient`。

### 2. Canal Adapter 投递 ES（推荐用于 Elasticsearch）

Canal Server 不直接写 ES，使用官方 `canal-adapter`：

```yaml
canal.conf:
  mode: tcp
  canalServerHost: canal-server:11111
  srcDataSources:
    defaultDS:
      url: jdbc:mysql://mysql:3306/demo
      username: canal
      password: ******
  canalAdapters:
    - instance: example
      groups:
        - groupId: g1
          outerAdapters:
            - name: es8
              hosts: http://es:9200
```

`es8/order.yml` mapping：

```yaml
dataSourceKey: defaultDS
destination: example
groupId: g1
esMapping:
  _index: orders
  _id: _id
  sql: "select id as _id, user_id, amount, status from orders"
  commitBatch: 3000
```

Adapter 负责 mapping、INSERT/UPDATE/DELETE、DDL 感知和批量写入；这是配置驱动，不是 Java 业务代码。

### 3. 自定义 CanalSink（需要业务转换时）

如果必须在 Java 里做字段脱敏、路由、聚合，再写 MQ/ES，本模块只提供扩展点：

```java
@Component
public class KafkaCanalSink implements CanalSink {
    private final KafkaTemplate<String, String> kafka;

    public void write(List<CanalRowEvent> events) {
        for (CanalRowEvent event : events) {
            kafka.send("ecology-cdc", event.table(), toJson(event));
        }
    }
}
```

ES 自定义写入同理，用 `ElasticsearchClient` 做 bulk；但优先评估 Canal Adapter，避免自己重复处理 mapping/DDL。

**结论**：MQ 有原生 Server 模式；ES 有 Adapter；只有需要业务逻辑时才写自定义 `CanalSink`。不是“只能日志”，也不是“所有都写代码”。
## 生产注意事项

- **ack 必须在业务处理成功后**：先 ack 再处理会丢事件；失败必须 rollback。
- **消费必须幂等**：Canal 可能重复投递；下游用主键/版本号/去重表。
- **批大小和超时**：batch 太大增加处理/回滚成本，太小增加 RTT；按 TPS 调。
- **DDL 要单独处理**：列变更会影响下游 schema，不能只当日志。
- **监控延迟**：Canal 自身有 lag/position；客户端还要看 lastBatchId、处理耗时、错误率。
- **Canal Server 不是本模块**：需要独立部署 Server、MySQL 账号、binlog 权限和 HA。
- **Cluster 模式**：ZooKeeper 负责选主，客户端不要自己抢同一个 destination。
- **过滤规则**：用 `schema.table` 正则；全库订阅会产生大量无用事件。
- **binlog 保留时间**：下游长时间停机可能被 MySQL 清理，需监控并告警。

## 未覆盖/未验证

- 未部署 Canal Server/MySQL，未做真实 binlog 收发。
- 未做下游 MQ/ES/DB 写入和 Exactly-Once。
- 未做 Canal Admin、HA 故障切换、lag/position 压测。

## 参考

- [Canal](https://github.com/alibaba/canal)
- [Canal Client](https://github.com/alibaba/canal/wiki/ClientAPI)
- 本项目根 README 的统一模块约定
