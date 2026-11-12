# java-ecology-sharding-jdbc

ShardingSphere-JDBC 生产用法模块：2 库 × 2 表分片、逻辑表 SQL、分片键路由、跨库查询和 H2/MySQL 双环境配置。

## 定位

覆盖研发/生产中的核心场景：

- ShardingSphere-JDBC 5.5.1，直接构建 `DataSource`
- 数据库分片：`user_id` 决定 `ds_0/ds_1`
- 表分片：`order_id` 决定 `t_order_0/t_order_1`
- 逻辑表 `t_order` 对业务透明，物理表自动路由
- 跨库查询：按 `user_id` 查询会广播到两个库
- 精确查询：`order_id + user_id` 只路由到一个物理表
- 双库独立 Hikari 池
- 分片键/主键不一致、Binding Table、Hint、读写分离、影子库
- H2 本地可运行，`mysql` profile 按生产双库配置

不包含：分布式事务（Seata 后续模块）、自动化数据迁移工具、数据加密/脱敏。扩容/影子表迁移给出生产方案，不在启动时执行迁移。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | Web、JDBC、Actuator |
| ShardingSphere-JDBC 5.5.1 | 分片数据源、SQL 路由 |
| HikariCP | 每个物理库一个连接池 |
| H2 | 本地分片 2 个内存库；readwrite profile 4 个内存库 |
| MySQL | `mysql` profile 使用 |

## 目录结构

```text
java-ecology-sharding-jdbc
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java/com/loveeleve/ecology/sharding
   │  │  ├─ ShardingApplication.java
   │  │  ├─ common/          ApiResponse
   │  │  ├─ algorithm/       Database/Table Hint 算法
   │  │  ├─ config/          ShardingDataSourceConfig、ReadWriteShardingConfig
   │  │  ├─ controller/      OrderController、OrderItemController、HintOrderController、ShardingController
   │  │  ├─ dto/             请求/响应 DTO
   │  │  ├─ exception/       OrderNotFoundException、GlobalExceptionHandler
   │  │  ├─ model/           Order、OrderItem
   │  │  ├─ repository/      OrderRepository、OrderItemRepository
   │  │  └─ service/         HintOrderService
   │  └─ resources
   │     ├─ sharding.yaml   分片规则
   │     ├─ sharding-readwrite.yaml 读写分离规则
   │     ├─ sharding-shadow.yaml 影子库规则
   │     ├─ application.yml
   │     ├─ application-mysql.yml
   │     ├─ application-readwrite.yml
   │     ├─ application-shadow.yml
   │     └─ db/             H2/MySQL 物理表结构
   └─ test/java/com/loveeleve/ecology/sharding
      ├─ ShardingOrderTests.java
      ├─ ReadWriteShardingTests.java
      └─ ShadowShardingTests.java
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| 2 库 × 2 表分片 | `OrderRepository` + `sharding.yaml` | `resources/sharding.yaml`、`config/ShardingDataSourceConfig.java` | `ShardingOrderTests` |
| 分片键/主键不一致 | `t_order_item` + `OrderItemRepository` | `resources/sharding.yaml`、`repository/OrderItemRepository.java` | `ShardingOrderTests` |
| Binding Table | `t_order,t_order_item` | `resources/sharding.yaml` | `ShardingOrderTests` |
| Hint | `HintOrderService` / `HintController` | `service/HintOrderService.java`、`algorithm/` | `ShardingOrderTests` |
| 读写分离 | `ReadWriteShardingConfig` | `config/ReadWriteShardingConfig.java`、`sharding-readwrite.yaml` | `ReadWriteShardingTests` |
| 影子库 | `ShadowShardingConfig` | `config/ShadowShardingConfig.java`、`sharding-shadow.yaml` | `ShadowShardingTests` |
| 扩容计划 | `ShardExpansionService` | `service/ShardExpansionService.java`、`controller/ExpansionController.java` | `ShardingOrderTests` |
| MySQL 读写分离/影子 | profile 配置 | `application-readwrite-mysql.yml`、`application-shadow-mysql.yml` | 未连真实 MySQL |

## 分片模型

```text
逻辑表: t_order
物理表: ds_0.t_order_0 / ds_0.t_order_1
        ds_1.t_order_0 / ds_1.t_order_1

database: ds_${user_id % 2}
table:    t_order_${order_id % 2}
```

路由示例：

| order_id | user_id | 物理库 | 物理表 |
|---|---|---|---|
| 1 | 1 | ds_1 | t_order_1 |
| 2 | 2 | ds_0 | t_order_0 |
| 3 | 1 | ds_1 | t_order_1 |
| 4 | 2 | ds_0 | t_order_0 |

`GET /api/sharding/plan?orderId=1&userId=1` 可以直接查看路由结果。

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/orders` | 写入逻辑表 `t_order` |
| GET | `/api/orders/{orderId}?userId=` | 精确路由查询 |
| GET | `/api/orders?userId=` | 按用户跨库查询 |
| PUT | `/api/orders/{orderId}/status?userId=` | 更新状态 |
| GET | `/api/sharding/plan?orderId=&userId=` | 查看分片路由 |
| POST | `/api/order-items` | 写入明细表：PK 与分片键不一致 |
| GET | `/api/order-items/{itemId}` | 无分片键查询，会广播 |
| GET | `/api/order-items?orderId=&userId=` | 按分片键查明细 |
| POST | `/api/hint/orders?database=&table=` | Hint 强制路由写 |
| GET | `/api/hint/orders/{orderId}?database=&table=` | Hint 强制路由读 |
| GET | `/actuator/health` | 健康检查 |

## 核心覆盖矩阵

| 能力 | 状态 | 位置 |
|---|---|---|
| 2 库 × 2 表分片 | ✅ 代码 + 测试 | `sharding.yaml`、`ShardingOrderTests` |
| 分片键与主键不一致 | ✅ 代码 + 测试 | `t_order_item`：PK `item_id`，分片键 `order_id` |
| Binding Table | ✅ 代码 + 测试配置 | `bindingTables: t_order,t_order_item` |
| Hint 强制路由 | ✅ 代码 + 测试 | `t_order_hint` + 自定义 HINT 算法 |
| 读写分离 | ✅ 代码 + 测试 | `readwrite` profile、`sharding-readwrite.yaml` |
| 读写强制走主库 | ✅ 代码 + 测试 | `HintManager.setWriteRouteOnly()` |
| 影子库 `!SHADOW` | ✅ 代码 + 测试 | `shadow` profile、`sharding-shadow.yaml` |
| 扩容 | ⚠️ 方案与边界 | 本文“扩容与影子表” |
| 影子表迁移 | ⚠️ 方案与边界 | 本文“扩容与影子表” |
| 分布式事务 | ❌ 不覆盖 | 后续 Seata 模块 |
| 数据加密/脱敏 | ❌ 不覆盖 | ShardingSphere 数据加密规则 |

## 分片键与主键不一致

这是最容易在面试和线上踩坑的点：

- `t_order`：主键 `order_id`，DB 分片键 `user_id`，表分片键 `order_id`。
  - 只按 `order_id` 查询，缺少 `user_id` 时无法定位数据库，会广播到两个库。
  - 唯一键必须全局唯一（Snowflake/号段），不能依赖单库自增。
- `t_order_item`：主键 `item_id`，分片键 `order_id`/`user_id`。
  - 按 `item_id` 查询只能广播；高频按 `item_id` 查要额外建索引表或调整分片键。
  - `t_order` 与 `t_order_item` 配成 Binding Table 后，按 `order_id + user_id` 关联查询可落到同一分片。

## 扩容与影子表

`% 2` 扩到 `% 4` 不能只改 `algorithm-expression`，否则历史数据全部错位。生产迁移顺序：

1. **建影子表**：`t_order_2/3` 或在 `ds_2/3` 建同构表，保留旧表。
2. **双写**：业务写旧路由和新路由（事务内双写或可靠消息最终一致）。
3. **回填**：按分片键批量迁移历史数据，记录 checkpoint，可重入。
4. **校验**：行数、主键集合、金额/状态 checksum、抽样对比。
5. **影子读**：按用户/租户灰度读新表，观察差异。
6. **切流**：路由规则切到新算法，保留旧表只读。
7. **回滚**：切流前保留旧路由开关；异常时切回。
8. **清理**：稳定后下线旧表。

影子表不是 ShardingSphere `!SHADOW` 规则的替代：`!SHADOW` 是生产/影子库联调能力，数据迁移影子表是迁移工程手段。

## 读写分离

`readwrite` profile 使用 4 个物理库：

```text
ds_0_master / ds_0_slave
ds_1_master / ds_1_slave
```

规则组合：

```yaml
- !READWRITE_SPLITTING
  dataSourceGroups:
    ds_0:
      writeDataSourceName: ds_0_master
      readDataSourceNames: [ds_0_slave]
      loadBalancerName: round_robin
- !SHARDING
  tables:
    t_order:
      actualDataNodes: ds_${0..1}.t_order_${0..1}
```

写入走 master，普通查询走 slave；复制延迟会导致“写完立即读不到”。需要强一致读时用 `HintManager.setWriteRouteOnly()` 强制主库。

## 影子库（ShardingSphere `!SHADOW`）

`shadow` profile 用两个物理库：

```text
ds_prod  生产库
ds_shadow 影子库
```

规则：

```yaml
- !SINGLE
  tables:
    - "*.*"          # 先注册逻辑表，否则 SHADOW 找不到 t_order
- !SHADOW
  dataSources:
    shadow_ds:
      productionDataSourceName: ds_prod
      shadowDataSourceName: ds_shadow
  tables:
    t_order:
      dataSourceNames: [shadow_ds]
      shadowAlgorithmNames: [user-id-insert-match, user-id-select-match]
  shadowAlgorithms:
    user-id-insert-match:
      type: REGEX_MATCH
      props: {regex: '[1]', column: user_id, operation: insert}
```

测试行为：`user_id=1` 的写/读路由到影子库，`user_id=2` 路由生产库。这个能力适合生产/影子库联调、灰度验证 SQL 与数据变更，不是数据迁移工具。

## Hint 强制路由

`t_order_hint` 使用自定义 HINT 算法：

```java
try (HintManager hintManager = HintManager.getInstance()) {
    hintManager.addDatabaseShardingValue("t_order_hint", database);
    hintManager.addTableShardingValue("t_order_hint", table);
    ...
}
```

注意：`setDatabaseShardingValue` 会清空 table hint；同时指定库和表时用 `addDatabaseShardingValue` + `addTableShardingValue`，不要混用。

## 本地 H2

```yaml
app:
  sharding:
    init-schema: true
    datasources:
      ds0:
        url: jdbc:h2:mem:shard-ds0;MODE=MySQL;DB_CLOSE_DELAY=-1...
      ds1:
        url: jdbc:h2:mem:shard-ds1;MODE=MySQL;DB_CLOSE_DELAY=-1...
```

`ShardingDataSourceConfig` 先初始化两个物理库的表结构，再创建 ShardingSphere `DataSource`；物理表必须在 ShardingSphere 加载元数据前存在。

### 生产 MySQL

```bash
export MYSQL_DS0_HOST=...
export MYSQL_DS1_HOST=...
export MYSQL_DS0_USER=...
export MYSQL_DS1_USER=...
export MYSQL_DS0_PASSWORD=...
export MYSQL_DS1_PASSWORD=...

java -jar java-ecology-sharding-jdbc/target/java-ecology-sharding-jdbc.jar \
  --spring.profiles.active=mysql
```

`mysql` profile：

- `app.sharding.init-schema=false`，禁止应用 `DROP/CREATE` 物理表
- 物理表由 Flyway/Liquibase/DBA 迁移
- 两库 Hikari 参数继承默认配置，可按库覆盖

### 分片规则

`sharding.yaml`：

```yaml
tables:
  t_order:
    actualDataNodes: ds_${0..1}.t_order_${0..1}
    databaseStrategy:
      standard:
        shardingColumn: user_id
        shardingAlgorithmName: order_database_inline
    tableStrategy:
      standard:
        shardingColumn: order_id
        shardingAlgorithmName: order_table_inline
```

生产可把 `INLINE` 换成 `MOD`、`HASH_MOD`、`COMPLEX_INLINE`、自定义 `StandardShardingAlgorithm`。

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 本地 H2 分片
./mvnw -pl java-ecology-sharding-jdbc spring-boot:run

# 本地 H2 分片 + 读写分离
./mvnw -pl java-ecology-sharding-jdbc spring-boot:run   -Dspring-boot.run.profiles=readwrite

# MySQL
./mvnw -pl java-ecology-sharding-jdbc clean package
java -jar java-ecology-sharding-jdbc/target/java-ecology-sharding-jdbc.jar \
  --spring.profiles.active=mysql
```

## 验证

```bash
# 写入：user_id=1 -> ds_1，order_id=31 -> t_order_1
curl -s -X POST http://localhost:8088/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"orderId":31,"userId":1,"amount":3100,"status":"CREATED"}'

# 精确路由
curl -s 'http://localhost:8088/api/orders/31?userId=1'

# 跨库查询
curl -s 'http://localhost:8088/api/orders?userId=1'

# 更新
curl -s -X PUT 'http://localhost:8088/api/orders/31/status?userId=1&status=PAID'

# 路由计划
curl -s 'http://localhost:8088/api/sharding/plan?orderId=31&userId=1'

# 明细：item_id 主键，order_id 分片键
curl -s -X POST http://localhost:8088/api/order-items \
  -H 'Content-Type: application/json' \
  -d '{"itemId":101,"orderId":31,"userId":1,"productName":"keyboard","quantity":2}'

# Hint 强制 ds_0 + t_order_hint_1
curl -s -X POST 'http://localhost:8088/api/hint/orders?database=0&table=1' \
  -H 'Content-Type: application/json' \
  -d '{"orderId":51,"userId":99,"amount":5100,"status":"CREATED"}'

# 健康检查
curl -s http://localhost:8088/actuator/health
```

`readwrite` profile 测试会用 slave 预置数据验证读写分离；`shadow` profile 测试验证 user_id=1 走影子库、user_id=2 走生产库。

## MySQL 读写分离/影子库配置

已有 MySQL profile 配置，未连接真实 MySQL 验证：

```bash
# 分片 + 读写分离（4 个 MySQL 库）
java -jar java-ecology-sharding-jdbc/target/java-ecology-sharding-jdbc.jar \
  --spring.profiles.active=readwrite,mysql-readwrite

# 分片 + 影子库（生产/影子 MySQL）
java -jar java-ecology-sharding-jdbc/target/java-ecology-sharding-jdbc.jar \
  --spring.profiles.active=shadow,mysql-shadow
```

对应环境变量见 `application-readwrite-mysql.yml`、`application-shadow-mysql.yml`；`init-schema=false`，表结构由迁移工具管理。

扩容计划代码：`GET /api/sharding/expansion/plan?userId=&orderId=&targetDatabases=4&targetTables=4`，返回当前/目标/影子库表位置和迁移步骤；它只生成计划，不执行数据迁移。已有单元测试验证目标位置计算。

## 生产注意事项

- **分片键一旦确定不要随意改**：`user_id`/`order_id` 变更会导致数据迁移和路由错位。
- **精确查询必须带分片键**：只查 `order_id` 不带 `user_id` 会广播到所有库表；高频查询要确保分片键在条件里。
- **跨库 JOIN 不支持或代价高**：优先按分片键冗余/拆查询/应用层聚合，不要依赖 ShardingSphere 跨库 JOIN；Binding Table 只保证同分片路由，不保证跨库能力。
- **分片键与主键不一致要有索引表**：`t_order_item` 的 `item_id` 查询会广播；按 `item_id` 高频查要建映射表，不能假设 ShardingSphere 会替你解决。
- **Hint 是运维/应急手段**：Hint 绕过正常分片算法，必须校验参数并有超时/审计；不要把它变成业务常规路由。
- **读写分离有延迟**：普通读默认 slave，写完立刻读可能为空；强一致读用 `setWriteRouteOnly()`，但会牺牲读扩展。
- **影子表要可回滚**：双写、回填、校验、灰度、切流、清理每一步都要有开关和 checkpoint；不要在生产直接 `ALTER` 路由。
- **`!SHADOW` 不是迁移工具**：它按算法把 SQL 路由到影子库，适合联调/灰度；历史数据迁移仍要双写、回填、校验。
- **影子规则匹配要谨慎**：`REGEX_MATCH` 按列值/操作匹配，配置错误会把生产流量打到影子库；先在测试库验证规则。
- **跨库事务不是本地事务**：ShardingSphere 本地事务只保证单库；跨库一致性用 Seata/业务补偿/最终一致。
- **主键生成要全局唯一**：本模块用业务传入 `order_id` 演示路由；生产应使用 Snowflake/号段/分布式 ID，不能依赖单库自增。
- **物理表结构要同步**：每个库的 `t_order_0/1` 必须一致；用迁移工具统一管理，禁止手工只改一个库。
- **扩容需要数据迁移**：`% 2` 改成 `% 4` 不是改配置就完事，要做双写/迁移/校验/切流。
- **SQL 兼容边界**：`DISTINCT`、`GROUP BY`、`UNION`、子查询、跨库排序分页要按 ShardingSphere 支持范围验证。
- **`sql-show` 生产关闭**：避免日志放大和 SQL 泄漏；排查时临时打开。
- **连接池按物理库计算**：两个库的 `maximumPoolSize` 之和要小于 MySQL 总连接预算。
- **读写分离另配**：ShardingSphere 的 `!READWRITE_SPLITTING` 或中间件层实现，不要和分片规则混在一起调试。

## 未覆盖/未验证

- MySQL 读写分离/影子库只有 profile 配置，未连接真实 MySQL。
- 扩容计划只生成目标位置和步骤，不执行双写/回填/切流。
- Seata 跨库事务、数据加密/脱敏未实现。

## 参考

- [ShardingSphere-JDBC 官方文档](https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-jdbc/)
- [ShardingSphere YAML 配置](https://shardingsphere.apache.org/document/current/en/user-manual/shardingsphere-jdbc/yaml-config/)
- [ShardingSphere 分片算法](https://shardingsphere.apache.org/document/current/en/user-manual/common-config/builtin-algorithm/sharding/)
- 本项目根 README 的统一模块约定
