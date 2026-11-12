# java-ecology-mybatis

MyBatis 在生产中的标准用法模块：XML Mapper、注解 Mapper、动态 SQL、ResultMap、TypeHandler、批量写入、事务、分页和慢 SQL 日志。

## 定位

覆盖研发/生产中的高频与中频场景：

- `UserMapper.xml`：ResultMap、动态 `<where>/<if>`、`<set>`、`<foreach>`、分页、批量写入
- `UserAnnotationMapper`：`@Select`、`@Results`、`@Insert`、`@Options(useGeneratedKeys)`
- 枚举与数据库整数列映射：自定义 `UserStatusTypeHandler`
- `@Transactional`：批量写入部分失败时整体回滚
- HikariCP：连接池大小、超时、空闲回收、生命周期、泄漏检测
- 慢 SQL 拦截：`Executor.query/update` 插件，超过阈值输出 warn
- REST 入口：创建、批量创建、查询、分页、更新、删除

不包含：前端页面、MyBatis-Plus、分库分表、多数据源、缓存框架。这些由后续模块单独覆盖。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | 自动配置、事务、Web、Actuator |
| MyBatis 3.5.x | `mybatis-spring-boot-starter` 3.0.5 |
| HikariCP | Spring Boot 默认连接池 |
| H2 | 仅用于本地运行与测试，MySQL 兼容模式 |
| MySQL | `mysql` profile 使用，生产连接基线 |
| Bean Validation | 请求参数校验 |

## 目录结构

```text
java-ecology-mybatis
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java/com/loveeleve/ecology/mybatis
   │  │  ├─ MyBatisApplication.java
   │  │  ├─ common/          ApiResponse
   │  │  ├─ config/          MyBatisConfig、SlowSqlProperties
   │  │  ├─ controller/      UserController
   │  │  ├─ dto/             请求、查询、分页、响应
   │  │  ├─ entity/          User
   │  │  ├─ enums/           UserStatus
   │  │  ├─ exception/       UserNotFoundException、GlobalExceptionHandler
   │  │  ├─ interceptor/     SlowSqlInterceptor
   │  │  ├─ mapper/          UserMapper、UserAnnotationMapper
   │  │  ├─ service/         UserService
   │  │  └─ typehandler/     UserStatusTypeHandler
   │  └─ resources
   │     ├─ application.yml
   │     ├─ application-dev.yml
   │     ├─ application-mysql.yml
   │     ├─ db/              H2/MySQL schema 与 H2 初始化数据
   │     └─ mapper/          UserMapper.xml
   └─ test/java/com/loveeleve/ecology/mybatis
      ├─ UserMapperTests.java
      ├─ UserServiceTests.java
      └─ UserApiTests.java
```

## 包职责

| 包 | 职责 |
|---|---|
| `mapper` | MyBatis 接口，XML 与注解两种实现 |
| `typehandler` | Java 枚举与 JDBC 类型转换 |
| `service` | 事务边界、业务编排 |
| `controller` | HTTP 入口，只做参数与响应 |
| `config` | Mapper 扫描、慢 SQL 插件、配置属性 |
| `interceptor` | MyBatis 插件（Executor 拦截） |
| `dto` | 请求校验、查询条件、分页与响应模型 |
| `entity` | 数据库实体 |
| `enums` | 业务枚举与状态码 |

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| XML Mapper / 动态 SQL | `UserMapper` + XML | `mapper/UserMapper.java`、`resources/mapper/UserMapper.xml` | `UserMapperTests` |
| 注解 Mapper | `UserAnnotationMapper` | `mapper/UserAnnotationMapper.java` | `UserMapperTests` |
| TypeHandler | `UserStatusTypeHandler` | `typehandler/`、`application.yml` | `UserMapperTests` |
| 分页 / 批量 / 事务 | `UserService` / `insertBatch` | `service/UserService.java`、`UserMapper.xml` | `UserServiceTests` |
| 慢 SQL | `SlowSqlInterceptor` | `interceptor/`、`config/` | 启动/配置检查 |
| Provider SQL | `UserProvider` | `provider/UserProvider.java`、`UserMapper.java` | `MyBatisAdvancedTests` |
| 二级缓存 | `UserCacheMapper` | `mapper/UserCacheMapper.java`、`application-cache.yml` | 未测（需开启 profile） |
| association/collection | `UserWithOrdersMapper.xml` | `model/UserWithOrders.java`、`resources/mapper/UserWithOrdersMapper.xml` | 未测（需 `t_order` 表） |
| BatchExecutor | `UserBatchService` | `service/UserBatchService.java` | `MyBatisAdvancedTests` |

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/users` | 创建用户，XML Mapper |
| POST | `/api/users/batch` | 批量创建，验证事务回滚 |
| GET | `/api/users/{id}` | 按 ID 查询，XML Mapper |
| GET | `/api/users/annotation/{id}` | 按 ID 查询，注解 Mapper |
| GET | `/api/users` | 动态条件 + 分页 |
| PUT | `/api/users/{id}` | 动态更新 |
| DELETE | `/api/users/{id}` | 删除 |
| GET | `/actuator/health` | 健康检查 |

查询参数：

```text
username、email：模糊匹配
status：ACTIVE / DISABLED
minAge、maxAge：年龄范围
page：页码，从 1 开始
size：每页数量，最大 100
```

## 配置说明

### 本地默认：H2

默认配置使用 H2 内存库，启动即可运行，不依赖外部 MySQL：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:ecology;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema-h2.sql
      data-locations: classpath:db/data-h2.sql
```

H2 仅用于本地演示和测试，不作为生产基线。生产使用 `mysql` profile。

### 生产：MySQL profile

```bash
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export MYSQL_DATABASE=ecology
export MYSQL_USER=ecology
export MYSQL_PASSWORD=change-me

java -jar java-ecology-mybatis/target/java-ecology-mybatis.jar \
  --spring.profiles.active=mysql
```

`application-mysql.yml` 关键点：

| 配置 | 值 | 说明 |
|---|---|---|
| `rewriteBatchedStatements` | `true` | 批量 INSERT 合并，减少网络往返 |
| `serverTimezone` | `Asia/Shanghai` | 显式时区，避免服务器时区不一致 |
| `useSSL` | `false` | 内网示例；生产按安全要求开启 TLS |
| `spring.sql.init.mode` | `never` | 生产禁止启动时自动建表/灌数据 |
| `maximum-pool-size` | `20` | 按 DB 最大连接数和实例数反推 |
| `minimum-idle` | `5` | 保持最小热连接 |
| `connection-timeout` | `3000ms` | 获取连接超时，必须小于上游超时 |
| `validation-timeout` | `1000ms` | 连接有效性校验 |
| `idle-timeout` | `600000ms` | 空闲连接回收 |
| `max-lifetime` | `1800000ms` | 小于 DB/代理的连接上限 |
| `keepalive-time` | `60000ms` | 防止中间件空闲断连；必须小于 `max-lifetime` |
| `leak-detection-threshold` | `20000ms` | 连接泄漏检测，仅用于告警 |
| `initialization-fail-timeout` | `1ms` | 启动时快速失败，避免半可用状态 |
| `transaction-isolation` | `TRANSACTION_READ_COMMITTED` | MySQL 常用隔离级别，降低间隙锁概率 |
| MySQL `cachePrepStmts` | `true` | 缓存 PreparedStatement，减少 SQL 解析 |
| MySQL `useServerPrepStmts` | `true` | 使用服务端预编译，配合缓存生效 |

连接池参数支持环境变量覆盖：`DB_POOL_MAX_SIZE`、`DB_POOL_MIN_IDLE`、`DB_POOL_CONNECTION_TIMEOUT_MS`、`DB_POOL_MAX_LIFETIME_MS`、`DB_POOL_LEAK_DETECTION_THRESHOLD_MS` 等。

生产建表脚本：`src/main/resources/db/schema-mysql.sql`。实际项目应由 Flyway/Liquibase 管理版本，不在应用启动时执行。

### MyBatis

| 配置 | 值 | 说明 |
|---|---|---|
| `mapper-locations` | `classpath*:mapper/**/*.xml` | XML Mapper 位置 |
| `type-aliases-package` | `...entity` | 实体别名 |
| `type-handlers-package` | `...typehandler` | 自定义 TypeHandler 自动注册 |
| `map-underscore-to-camel-case` | `true` | `created_at -> createdAt` |
| `default-statement-timeout` | `3` | SQL 语句超时（秒） |
| `local-cache-scope` | `statement` | 关闭跨语句一级缓存复用 |
| `cache-enabled` | `false` | 默认不开二级缓存，避免脏读 |
| `log-impl` | `Slf4jImpl` | SQL 日志交给 SLF4J |

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 本地 H2，默认
./mvnw -pl java-ecology-mybatis spring-boot:run

# 本地 H2 + SQL DEBUG 日志
./mvnw -pl java-ecology-mybatis spring-boot:run \
  -Dspring-boot.run.profiles=dev

# MySQL
./mvnw -pl java-ecology-mybatis clean package
java -jar java-ecology-mybatis/target/java-ecology-mybatis.jar \
  --spring.profiles.active=mysql
```

## 验证

```bash
# 创建
curl -s -X POST http://localhost:8081/api/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"dave","email":"dave@example.com","age":30}'

# 动态查询 + 分页
curl -s 'http://localhost:8081/api/users?username=ali&status=ACTIVE&page=1&size=10'

# 注解 Mapper
curl -s http://localhost:8081/api/users/annotation/1

# 批量创建
curl -s -X POST http://localhost:8081/api/users/batch \
  -H 'Content-Type: application/json' \
  -d '{"users":[{"username":"eve","email":"eve@example.com","age":25},{"username":"frank","email":"frank@example.com","age":26}]}'

# 校验失败
curl -s -X POST http://localhost:8081/api/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"","email":"bad"}'

# 健康检查
curl -s http://localhost:8081/actuator/health

# HikariCP 指标（pool 名与配置一致）
curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active
curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.max
curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.pending
```

## 生产注意事项

- **连接池不是越大越好**：`maximum-pool-size` 要按 DB `max_connections`、应用实例数、下游耗时共同估算；连接池过大只会把压力推给数据库。
- **超时要短于上游**：`connection-timeout`、MyBatis `default-statement-timeout`、数据库 `innodb_lock_wait_timeout`、接口超时要形成递减关系。
- **批量写入必须开事务**：`insertBatch` 任何一行失败都要整体回滚，避免“写了一半”的脏数据。
- **批量 SQL 注意长度**：`<foreach>` 生成的 SQL 过长会触发 `max_allowed_packet`；大批量应分片提交，不能无限拼。
- **禁止 `ORDER BY ${column}`**：动态排序如果必须支持，要用白名单映射，不能直接拼接用户输入。
- **二级缓存默认关闭**：多实例部署下本地二级缓存会脏读；需要缓存用 Redis 等集中式方案。
- **慢 SQL 日志只做告警**：插件输出 SQL id 和耗时，不打印参数中的敏感信息；阈值按 P99 设置。
- **TypeHandler 要覆盖 null 与未知值**：数据库出现非法状态码时应快速失败，而不是静默映射成默认值。
- **生产禁用 `sql.init`**：建表、索引、数据初始化交给迁移工具；应用启动只负责连接和校验。
- **H2 不是生产数据库**：H2 的 MySQL 模式只用于测试 SQL 形状，SQL 方言、锁、隔离级别、执行计划仍需在真实 MySQL 上验证。
- **Druid 不放在本模块**：Druid 的 StatView、WallFilter、监控和安全配置是独立能力，统一放到 `java-ecology-druid`；MyBatis 模块固定展示 Spring Boot 默认 HikariCP。

## 高级用法代码（未全部跑测试）

这些代码已实现；Provider SQL 和 BatchExecutor 已纳入 H2 测试，Cache/association 依赖表结构未纳入全量测试：

- **二级缓存**：`UserCacheMapper` 使用 `@CacheNamespace`；启用 profile `cache` 打开 `mybatis.configuration.cache-enabled=true`。生产多实例慎用本地二级缓存。
- **Provider SQL**：`UserProvider` + `@SelectProvider`，见 `UserMapper.selectByEmailProvider`。
- **association/collection**：`UserWithOrdersMapper.xml` 用 `<collection select="selectOrdersByUserId">` 做嵌套查询；需要 `t_order` 表。
- **BatchExecutor**：`UserBatchService.batchInsert` 用 `ExecutorType.BATCH` + `flushStatements/commit`。
- `application-cache.yml` 是二级缓存开关示例。

## 未覆盖/未验证

- 二级缓存只提供 `@CacheNamespace` 示例，未做多实例缓存一致性测试。
- association/collection 依赖 `t_order` 表，未纳入全量测试。
- 存储过程、`@SelectProvider` 动态 SQL 的复杂分支未覆盖。

## 参考

- [MyBatis 官方文档](https://mybatis.org/mybatis-3/)
- [mybatis-spring-boot-starter](https://github.com/mybatis/spring-boot-starter)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
- [whatsmars](https://github.com/javahongxi/whatsmars) 的模块化组织
- 本项目根 README 的统一模块约定
