# java-ecology-sofa-jraft

Sofa-Jraft / JRaft 模块：3 节点 Raft 集群、Leader 选举、KV 状态机复制、快照、线性一致写读。

## 定位

覆盖 JRaft 生产使用的核心链路：

- 3 节点 Raft 集群
- Leader 选举
- `KvStateMachine`：PUT / GET
- Leader 提交日志，Follower 复制
- `Closure` / `Task` 异步提交
- 线性一致读（GET 也走 Raft 日志）
- Snapshot save/load
- 节点启动/关闭
- 非 Leader 返回 Leader 地址

不包含：多机房部署、Raft 成员变更、只读线性一致优化、RheaKV、分布式事务、生产监控面板。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Sofa-Jraft 1.4.1 | Raft 实现 |
| Spring Boot 3.2.5 | Web/Actuator |
| Bolt RPC | JRaft 节点通信 |
| RocksDB | JRaft log 存储 |
| JUnit 5 | 3 节点真实集群测试 |

## 目录结构

```text
java-ecology-sofa-jraft
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/jraft
   │  ├─ JraftApplication.java
   │  ├─ config/JraftProperties.java
   │  ├─ controller/JraftController.java
   │  ├─ model/KvCommand.java
   │  ├─ service/JraftNode.java
   │  ├─ service/JraftCluster.java
   │  ├─ service/JraftNodeLifecycle.java
   │  ├─ service/KvClosure.java
   │  └─ service/KvStateMachine.java
   ├─ main/resources/application.yml
   └─ test/java/...（3 节点集群 + 上下文）
```

## 写读流程

```text
PUT key=value
  ↓
Leader.apply(Task(KvCommand))
  ↓
Raft log replicate to majority
  ↓
KvStateMachine.onApply
  ↓
Closure.complete(result)
```

GET 也通过 Raft 日志执行，保证线性一致；这是示例的简化选择，生产高吞吐读可另做 ReadIndex/LeaseRead。

## 配置

```yaml
app:
  jraft:
    enabled: ${JRAFT_ENABLED:false}
    group-id: ${JRAFT_GROUP_ID:ecology-kv}
    node-id: ${JRAFT_NODE_ID:127.0.0.1:19991}
    peers: ${JRAFT_PEERS:127.0.0.1:19991}
    data-path: ${JRAFT_DATA_PATH:/tmp/ecology-jraft}
```

3 节点生产部署时，每个进程使用不同：

- `JRAFT_NODE_ID`：本机 `host:port`
- `JRAFT_PEERS`：三节点完整列表，例如 `node1:19991,node2:19991,node3:19991`
- `JRAFT_DATA_PATH`：独立持久化目录
- `JRAFT_ENABLED=true`

单机 demo 可以让 `node-id == peers`，但这不是高可用基线。

## HTTP 示例

```bash
# 写（必须打到 Leader）
curl -s -X POST http://localhost:8111/api/jraft/put \
  -H 'Content-Type: application/json' \
  -d '{"op":"PUT","key":"order-1","value":"PAID"}'

# 读
curl -s 'http://localhost:8111/api/jraft/get?key=order-1'

# 状态
curl -s http://localhost:8111/api/jraft/status
```

非 Leader 返回 307 和当前 Leader；生产客户端应重定向或维护 Leader 路由。

## 测试

```bash
./mvnw -pl java-ecology-sofa-jraft test
```

当前 2 个测试：

- 3 节点集群选举 Leader、PUT 复制到全部节点、GET 返回一致值
- Spring 上下文（JRAFT_ENABLED=false，不启动节点）

测试使用本机随机端口和临时目录，不依赖外部服务。

## 生产注意事项

- **至少 3 节点**：2 节点无法容忍任一节点故障；重要系统用 5 节点。
- **多数派可用**：失去多数派只能读旧状态或不可用，不能继续写。
- **磁盘必须可靠**：log/meta/snapshot 丢失会破坏恢复；用独立盘和备份。
- **快照要监控**：snapshot 失败会导致 log 无限增长和恢复变慢。
- **Leader 路由**：客户端必须知道 Leader；非 Leader 返回重定向，不能默默写本地。
- **读一致性**：本示例 GET 走日志；高吞吐场景再评估 ReadIndex/LeaseRead。
- **网络与时钟**：Raft 依赖稳定网络和合理 election timeout；跨机房要单独评估。
- **成员变更**：生产扩缩容要走 JRaft 的配置变更流程，不要直接改 peers。
- **JRaft 版本与日志**：1.4.1 在 Boot 3 下会打印 sofa-common-tools 日志初始化警告，不影响测试；生产建议固定版本并验证日志兼容性。

## 未覆盖/未验证

- 未做 3 个独立进程/多机部署、网络分区、节点故障恢复。
- 未做成员变更、RheaKV、ReadIndex/LeaseRead 优化。
- 未做 RocksDB 容量、快照、长稳和压测。

## 参考

- [Sofa-Jraft](https://github.com/sofastack/sofa-jraft)
- [JRaft User Guide](https://www.sofastack.tech/projects/sofa-jraft/overview/)
- 本项目根 README 的统一模块约定
