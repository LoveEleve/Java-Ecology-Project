# java-ecology-seata

Seata AT 分布式事务模块：Order 发起全局事务，Account 扣款，Storage 扣库存；TC 负责全局提交/回滚。

## 定位

覆盖生产最常用的 Seata AT 核心链路：

- `@GlobalTransactional` 发起方
- Order / Account / Storage 三个 RM 分支事务
- Feign 调用透传 `TX_XID`
- `undo_log` 自动回滚
- 请求级幂等（`request_id` 唯一键）
- 业务校验 + 远程失败快速失败
- Nacos 服务发现 + Seata Nacos registry/config（可选 profile）
- Seata TC 集群、DB 存储、重试参数口径

不包含：TCC、SAGA、XA 的完整业务示例；本模块只实现 AT，避免把多种事务模式混在一个 demo 里。

## 架构

```text
                 ┌────────────────────┐
                 │  Seata TC (8091)   │
                 │  cluster + DB mode │
                 └─────────┬──────────┘
                           │ commit/rollback
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
   │ Order TM/RM │  │Account RM   │  │ Storage RM  │
   │   :8100     │  │   :8101     │  │   :8102     │
   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
          │ Feign TX_XID    │                │
          └────────────────┴────────────────┘
                  MySQL + undo_log
```

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web、JDBC、Actuator |
| Spring Cloud Alibaba 2023.0.1.2 | Nacos Discovery |
| Spring Cloud OpenFeign 2023.0.3 | Order → Account/Storage |
| Apache Seata 2.5.0 | AT、TM、RM、TC client |
| MySQL 8 | 生产数据库；H2 仅本地测试/上下文 |
| Nacos 2.x | 服务发现、Seata registry/config |

## 目录结构

```text
java-ecology-seata
├─ pom.xml                         # 聚合 POM + Seata/Spring Cloud BOM
├─ README.md
├─ seata-api/                      # 共享 DTO
├─ seata-common/                   # TX_XID Web 透传
├─ seata-order-service/            # TM + 订单 RM，端口 8100
├─ seata-account-service/          # 账户 RM，端口 8101
└─ seata-storage-service/          # 库存 RM，端口 8102
```

## 接口

### Order

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `http://localhost:8100/api/orders` | 创建订单，开启全局事务 |

请求：

```json
{
  "requestId": "req-1001",
  "userId": 1,
  "productId": 1,
  "amount": 100.00,
  "quantity": 2
}
```

响应：

```json
{"orderId":1,"status":"CONFIRMED"}
```

### Account

`POST http://localhost:8101/internal/accounts/debit`

```json
{"requestId":"req-1001","userId":1,"amount":100.00}
```

### Storage

`POST http://localhost:8102/internal/storages/deduct`

```json
{"requestId":"req-1001","productId":1,"quantity":2}
```

## 核心实现

### 1. 全局事务

`OrderService.create`：

```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class, timeoutMills = 60000)
@Transactional
public OrderResponse create(CreateOrderRequest request) {
    // 本地订单 + Feign 扣款 + Feign 扣库存
}
```

- Order 本地插入订单
- Account 扣款，注册 AT 分支
- Storage 扣库存，注册 AT 分支
- 任一失败，TC 回滚已提交的 Account/Storage 本地事务

### 2. XID 透传

Order 的 Feign 拦截器：

```java
String xid = RootContext.getXID();
if (xid != null) {
    template.header(RootContext.KEY_XID, xid);
}
```

Account/Storage 的 Web 拦截器从 `TX_XID` 头绑定 `RootContext`，保证 RM 能加入同一个全局事务。

### 3. 幂等

- `order_tbl.request_id` 唯一
- `account_txn.request_id` 唯一
- `storage_txn.request_id` 唯一
- 重复请求：已 `SUCCESS` 直接返回；处理中返回 409，避免重复扣款/扣库存

### 4. 数据库

每个业务库必须有 `undo_log`，否则 AT 无法回滚：

- `order_tbl` + `undo_log`
- `account_tbl` + `account_txn` + `undo_log`
- `storage_tbl` + `storage_txn` + `undo_log`

H2 schema 仅用于上下文/单测；Seata AT 实际使用 MySQL。

## 配置

默认使用文件 registry/config + 直连 TC：

```yaml
seata:
  enabled: ${SEATA_ENABLED:false}   # 本地默认关闭；生产显式设 true
  tx-service-group: ecology_tx_group
  service:
    vgroup-mapping:
      ecology_tx_group: default
    grouplist:
      default: ${SEATA_SERVER_ADDR:127.0.0.1:8091}
  registry:
    type: file
  config:
    type: file
  enable-auto-data-source-proxy: true
  data-source-proxy-mode: AT
```

本地启动三个服务和 TC（示意）：

```bash
export SEATA_ENABLED=true
export SEATA_SERVER_ADDR=127.0.0.1:8091
export ACCOUNT_SERVICE_URL=http://127.0.0.1:8101
export STORAGE_SERVICE_URL=http://127.0.0.1:8102
./mvnw -f java-ecology-seata/pom.xml -pl seata-account-service spring-boot:run
./mvnw -f java-ecology-seata/pom.xml -pl seata-storage-service spring-boot:run
./mvnw -f java-ecology-seata/pom.xml -pl seata-order-service spring-boot:run
```

本机没有启动 Seata TC、Nacos 和 MySQL，因此没有真实全局事务收发验证；默认 `SEATA_ENABLED=false`，不会在无 TC 时误连。

### Nacos + Seata 集群

使用 `nacos` profile：

```bash
export NACOS_SERVER=nacos-1:8848,nacos-2:8848
export NACOS_DISCOVERY_ENABLED=true
./mvnw -f java-ecology-seata/pom.xml -pl seata-order-service spring-boot:run -Dspring-boot.run.profiles=nacos
```

profile 会：

- 开启 Nacos Discovery
- 开启 Seata 并切换 registry/config 到 Nacos
- 注册 `seata-server` 集群
- 使用 `seataServer.properties` 配置 TC

生产 TC 必须：

- 多实例 + 注册中心
- DB 存储模式（不要 file）
- TC 数据库独立、连接池/超时/重试调优
- Nacos 集群、鉴权、命名空间隔离

## 本地测试

```bash
./mvnw -f java-ecology-seata/pom.xml test
```

当前 26 个测试：

- Account：扣款、重复请求、请求 ID 复用冲突、余额不足、处理中拒绝、本地事务回滚
- Storage：扣库存、重复请求、请求 ID 复用冲突、库存不足、处理中拒绝
- Order：本地落单 + 远程调用顺序、幂等、请求 ID 复用冲突、未确认订单拒绝、账户失败短路、参数校验、订单 Repository SQL
- XID：Web 入站绑定/清理、避免误清本地全局事务、Feign 出站 `TX_XID` 透传
- 三个服务的 Spring 上下文（`seata.enabled=false`，不起 TC/Nacos）

## 生产注意事项

- **AT 依赖 undo_log**：每库都要建；DDL 不覆盖（Seata AT 不支持 DDL 回滚）。
- **全局事务不要包太长**：Feign、慢 SQL、外部 IO 都会拉长锁持有时间；超时设置小于上游超时。Order 当前把本地 `@Transactional` 和 Feign 放在同一方法，生产应拆短本地事务/考虑 Outbox。
- **Feign 不要盲目重试**：`Retryer.NEVER_RETRY`，否则同一分支可能被重复提交；重试交给全局事务/业务幂等。
- **幂等键要全局唯一**：`request_id` 不能只用订单号；跨服务重复请求必须能识别。
- **XID 必须透传**：HTTP/Feign/MQ 任一跳丢失 `TX_XID`，RM 就会脱离全局事务；本模块只覆盖 HTTP/Feign，MQ 场景要单独处理。
- **幂等不仅看 requestId**：同一 requestId 的不同 payload 必须拒绝，本模块已加入字段比对；未确认的订单返回 409，不冒充成功。
- **TC 高可用**：TC 单点是生产事故点；多实例 + DB 存储 + Nacos 注册。
- **AT 锁冲突**：热点账户/库存会触发全局锁重试，需要评估 `retry-times`、隔离级别和业务拆分。
- **不要把所有事务都做成全局事务**：能本地事务/最终一致解决的，不要用 AT。
- **Nacos/Seata 版本要对齐**：Seata 2.5.x 与 Spring Cloud Alibaba 2023.x 配套；不要混用旧 `io.seata` starter。
- **H2 不是 AT 生产库**：H2 只用于本地上下文和单测。

## 未覆盖/未验证

- 未启动 Seata TC/Nacos/MySQL，未做真实 commit/rollback 验证。
- 未实现 TCC、SAGA、XA 示例。
- 未做 TC 集群压测、故障注入、undo_log 清理。
- 未做 MQ 场景的 XID 透传。

## 参考

- [Apache Seata](https://seata.apache.org/)
- [Seata AT mode](https://seata.apache.org/docs/user/mode/at/)
- 本项目根 README 的统一模块约定
