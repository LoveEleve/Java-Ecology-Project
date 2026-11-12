# java-ecology-druid

Druid 连接池的生产用法模块：池参数、连接校验、保活、慢 SQL、StatView、WebStatFilter、WallFilter 和运行时池状态。

## 定位

Druid 和 HikariCP 都是连接池，核心原理相通：预创建、借还、校验、保活、淘汰、泄漏检测。但 Druid 额外提供：

- **StatViewServlet**：连接池、SQL、URI、Session 的监控页面
- **WebStatFilter**：按 URI 统计 JDBC 交互
- **WallFilter**：SQL 防火墙/语法限制
- **StatFilter**：慢 SQL、SQL 合并统计
- **配置加密、密码回调**等企业特性

因此本模块独立存在，不塞进 MyBatis 或 MyBatis-Plus。ORM 负责 SQL 映射，连接池负责连接生命周期，两者分开演进。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | Web、JDBC、Actuator |
| Druid 1.2.28 | `druid-spring-boot-3-starter` |
| H2 | 本地运行与测试，MySQL 兼容模式 |
| MySQL | `mysql` profile 使用 |
| JdbcTemplate | 最小数据访问示例，避免和 MyBatis 模块重复 |

## 目录结构

```text
java-ecology-druid
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java/com/loveeleve/ecology/druid
   │  │  ├─ DruidApplication.java
   │  │  ├─ common/          ApiResponse
   │  │  ├─ controller/      PoolController
   │  │  ├─ dto/             CreateUserRequest、UserResponse、PoolStats
   │  │  ├─ exception/       UserNotFoundException、GlobalExceptionHandler
   │  │  ├─ model/           PoolUser
   │  │  ├─ repository/      PoolUserRepository（JdbcTemplate）
   │  │  └─ service/         PoolUserService、PoolStatsService
   │  └─ resources
   │     ├─ application.yml
   │     ├─ application-dev.yml
   │     ├─ application-mysql.yml
   │     └─ db/              H2/MySQL schema
   └─ test/java/com/loveeleve/ecology/druid
      ├─ DruidPoolTests.java
      └─ DruidApiTests.java
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| Druid 连接池 | `DruidDataSource` 自动配置 | `application.yml`、`application-mysql.yml` | `DruidPoolTests` |
| 池状态接口 | `PoolController` / `PoolStatsService` | `controller/PoolController.java`、`service/PoolStatsService.java` | `DruidApiTests` |
| StatView | `stat-view-servlet` | `application.yml`、`application-dev.yml` | 默认关闭；未做 dev 页面测试 |
| StatFilter / 慢 SQL | `filter.stat` | `application.yml` | 启动/配置检查 |
| WallFilter | `filter.wall` | `application-mysql.yml` | 仅 MySQL 配置，未实测 |
| WebStatFilter | `web-stat-filter` | `application.yml` | 启动/配置检查 |

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/users` | 通过 Druid 连接写入 |
| GET | `/api/users/{id}` | 查询用户 |
| GET | `/api/users/count` | 统计用户数 |
| GET | `/api/pool` | 返回 Druid 运行时池状态 |
| GET | `/actuator/health` | 健康检查 |
| GET | `/druid/index.html` | StatView，仅 `dev` 开启 |

`/api/pool` 返回字段：

```text
name、activeCount、poolingCount、maxActive、minIdle、waitThreadCount、
createCount、destroyCount、connectCount、errorCount、discardCount
```

## 连接池配置

| 配置 | 值 | 说明 |
|---|---|---|
| `initial-size` | `5` | 启动预创建连接，避免首批请求抖动 |
| `min-idle` | `5` | 最小空闲连接 |
| `max-active` | `20` | 最大连接数，按 DB 容量和实例数反推 |
| `max-wait` | `3000ms` | 获取连接超时，必须短于上游超时 |
| `time-between-eviction-runs-millis` | `60000ms` | 空闲检查周期 |
| `min-evictable-idle-time-millis` | `300000ms` | 最小淘汰空闲时间 |
| `max-evictable-idle-time-millis` | `900000ms` | 最大淘汰空闲时间 |
| `validation-query` | `SELECT 1` | 连接有效性校验 |
| `validation-query-timeout` | `1s` | 校验超时 |
| `test-while-idle` | `true` | 空闲时校验，避免拿到坏连接 |
| `test-on-borrow` | `false` | 借用时不再校验，降低延迟 |
| `test-on-return` | `false` | 归还时不再校验，降低延迟 |
| `keep-alive` | `true` | 保活，防止中间件空闲断连 |
| `keep-alive-between-time-millis` | `120000ms` | 必须大于 `time-between-eviction-runs-millis` |
| `phy-timeout-millis` | `1800000ms` | 物理连接最大存活时间 |
| `remove-abandoned` | `false` | Druid 自身提示不建议生产开启；泄漏检测优先用监控/告警 |
| `pool-prepared-statements` | `true` | 缓存 PreparedStatement |
| `max-pool-prepared-statement-per-connection-size` | `20` | 每连接缓存上限 |

### 为什么这些值不是随便填的

- `max-active` 不是越大越好：`应用实例数 × max-active` 必须小于 MySQL `max_connections` 减去预留连接；否则故障时会把数据库打满。
- `max-wait` 必须小于接口超时：连接池等待时间超过上游超时，用户已经失败，线程还在等连接。
- `keep-alive-between-time-millis` 必须大于 `time-between-eviction-runs-millis`，否则 Druid 启动直接报错；这是本模块实际踩到的配置约束。
- `phy-timeout-millis` 要小于 MySQL `wait_timeout`、负载均衡/代理的空闲断连时间，避免拿到已被服务端关闭的连接。
- `remove-abandoned` 容易误杀长事务，Druid 启动时会提示 `not use in production`；本模块默认关闭，连接泄漏通过 `activeCount`、`waitThreadCount` 和慢 SQL/调用链告警发现。

## 监控配置

### StatView

默认关闭，`dev` 开启：

```bash
./mvnw -pl java-ecology-druid spring-boot:run \
  -Dspring-boot.run.profiles=dev
```

访问 `http://localhost:8082/druid/index.html`。

生产不要直接暴露：

- 必须修改 `DRUID_STAT_USER` / `DRUID_STAT_PASSWORD`
- 通过网关/内网 ACL 限制来源
- 或直接关闭 `stat-view-servlet`，只保留指标和日志

### StatFilter

```yaml
filters: stat,slf4j
filter:
  stat:
    slow-sql-millis: 200
    log-slow-sql: true
    merge-sql: true
```

`merge-sql` 会把参数不同的同类 SQL 合并统计，避免监控页被大量 SQL 文本撑爆。

### WallFilter

WallFilter 只在 `mysql` profile 开启：

```yaml
filters: stat,wall,slf4j
filter:
  wall:
    enabled: true
    db-type: mysql
    throw-exception: true
    config:
      multi-statement-allow: false
      none-base-statement-allow: false
      drop-table-allow: false
      alter-table-allow: false
      truncate-allow: false
```

WallFilter 是最后一道防线，不是 SQL 注入的唯一防线；参数化 SQL、最小权限账号、输入校验仍然必须做。

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 本地 H2
./mvnw -pl java-ecology-druid spring-boot:run

# 本地 H2 + StatView
./mvnw -pl java-ecology-druid spring-boot:run \
  -Dspring-boot.run.profiles=dev

# MySQL
./mvnw -pl java-ecology-druid clean package
java -jar java-ecology-druid/target/java-ecology-druid.jar \
  --spring.profiles.active=mysql
```

## 验证

```bash
# 写入
curl -s -X POST http://localhost:8082/api/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"druid-user","email":"druid@example.com","age":30}'

# 查询
curl -s http://localhost:8082/api/users/1
curl -s http://localhost:8082/api/users/count

# 池状态
curl -s http://localhost:8082/api/pool

# 健康检查
curl -s http://localhost:8082/actuator/health

# dev 下查看 StatView
curl -s http://localhost:8082/druid/index.html
```

## 生产注意事项

- **HikariCP 与 Druid 不共存**：同一应用只选一个连接池；同时引入两个 starter 会导致 `DataSource` 装配不确定。
- **Druid 版本要匹配 Spring Boot 3**：必须用 `druid-spring-boot-3-starter`，不要用 Boot 2 的 `druid-spring-boot-starter`。
- **生产关闭 StatView 或加认证/ACL**：监控页会暴露 SQL、URI、连接信息，默认口令绝对不能上线。
- **WallFilter 先观察再拦截**：生产先 `log-violation` 观察，确认业务 SQL 都能通过后再 `throw-exception`，否则会误杀复杂 SQL。
- **慢 SQL 阈值按 P99**：200ms 只是示例；OLTP 通常更低，报表/批处理单独设置。
- **`removeAbandoned` 慎用**：会误杀长事务/批处理连接；优先用池指标、连接持有时间、调用链定位泄漏。
- **`max-active` 要和 DB 连接数一起算**：`实例数 × max-active + 预留` 不能超过 `max_connections`。
- **`validation-query` 有成本**：`test-while-idle` 已足够，不要同时开 `test-on-borrow` 和 `test-on-return`。
- **H2 不是生产数据库**：H2 只验证配置和 SQL 形状，真实 MySQL 的连接回收、锁、WallFilter 规则仍需实测。

## 未覆盖/未验证

- WallFilter 只有 MySQL profile 配置，未连接真实 MySQL 验证。
- StatView 只在 dev profile 开启，未做页面自动化测试。
- Druid 加密/密码回调未写示例。

## 参考

- [Druid 官方 Wiki](https://github.com/alibaba/druid/wiki)
- [druid-spring-boot-3-starter](https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
- 本项目根 README 的统一模块约定
