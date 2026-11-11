# java-ecology-multi-datasource

多数据源生产用法模块：静态多库 + 动态路由 + 注解切换 + 连接池隔离。路由主体用 `JdbcTemplate` 展示，另外用 MyBatis-Plus 验证 ORM 可以接在 routing DataSource 上。

## 定位

覆盖研发/生产中的核心场景：

- **静态多数据源**：`master`、`report` 两个独立 `DataSource`、`JdbcTemplate`，互不干扰
- **动态路由**：`AbstractRoutingDataSource` + `ThreadLocal` 上下文
- **注解切换**：`@DataSource(REPORT)`，AOP 在方法执行期间切换
- **编程式切换**：`DataSourceContextHolder.set(...)` + `finally` 清理
- **每库独立 Hikari 池**：连接数、超时、泄漏检测分别配置
- **事务注意事项**：路由必须在事务获取连接前生效

不包含：分库分表、跨库强一致事务、Seata、ShardingSphere。分片和分布式事务后续独立模块。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | Web、JDBC、AOP、Actuator |
| HikariCP | 每个数据源一个独立连接池 |
| MyBatis-Plus 3.5.7 | 动态路由 DataSource 上的 ORM 消费示例 |
| H2 | 本地运行与测试，两个独立内存库 |
| MySQL | `mysql` profile 使用，主库/报表库分开 |
| `AbstractRoutingDataSource` | Spring 原生动态路由 |

## 目录结构

```text
java-ecology-multi-datasource
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java/com/loveeleve/ecology/multids
   │  │  ├─ MultiDataSourceApplication.java
   │  │  ├─ config/          DataSourceKey、ContextHolder、RoutingDataSource、
   │  │  │                   @DataSource、Aspect、MultiDataSourceConfig
   │  │  ├─ controller/      MultiDataSourceController
   │  │  ├─ entity/          AccountEntity（MP）
   │  │  ├─ mapper/          AccountMapper（BaseMapper）
   │  │  ├─ model/           Account
   │  │  ├─ repository/      Master/Report/Dynamic AccountRepository
   │  │  └─ service/         StaticAccountService、DynamicAccountService、ClassLevelReportAccountService
   │  └─ resources
   │     ├─ application.yml
   │     ├─ application-dev.yml
   │     ├─ application-mysql.yml
   │     └─ db/              两套 H2/MySQL schema
   └─ test/java/com/loveeleve/ecology/multids
      ├─ MultiDataSourceTests.java
      └─ MultiDataSourceApiTests.java
```

## 两种多数据源方式

### 1. 静态多数据源

每个库有自己的 `DataSource`/`JdbcTemplate`，代码显式选择：

```java
public StaticAccountService(MasterAccountRepository masterRepository,
                            ReportAccountRepository reportRepository) {
    this.masterRepository = masterRepository;
    this.reportRepository = reportRepository;
}
```

适合：

- 主库 + 报表库
- 业务库 + 日志/审计库
- 不同业务域数据库

如果是 MyBatis/MyBatis-Plus 静态多库，需要为每个库建独立 `SqlSessionFactory`（MP 用 `MybatisSqlSessionFactoryBean`）+ `MapperScan(basePackages=..., sqlSessionFactoryRef=...)`，不能只换 `DataSource`。本模块动态示例已经验证 MP 消费 routing `DataSource` 的路径。

### 2. 动态路由

`RoutingDataSource` 在每次获取连接时决定目标库：

```java
public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getOrDefault();
    }
}
```

注解方式：

```java
@DataSource(DataSourceKey.REPORT)
public List<Account> reportAccountsByAnnotation() {
    return repository.findAll();
}
```

AOP 设置 `ThreadLocal`，`finally` 恢复/清理。`@Order(HIGHEST_PRECEDENCE)` 保证它在事务拦截器之前执行；否则事务可能已经从事务管理器拿到默认库连接。

MyBatis-Plus 直接消费 `@Primary` 的 routing `DataSource`：

```java
@DataSource(DataSourceKey.REPORT)
public List<Account> reportAccountsByMyBatisPlus() {
    return accountMapper.selectList(null)...;
}
```

`@DataSource + @Transactional` 的顺序由测试覆盖：路由先设置，事务再获取连接，因此事务内查询走报表库。

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/static/master/accounts` | 静态主库 |
| GET | `/api/static/report/accounts` | 静态报表库 |
| GET | `/api/dynamic/master/accounts` | 注解路由到主库 |
| GET | `/api/dynamic/report/accounts` | 注解路由到报表库 |
| GET | `/api/dynamic/mp/report/accounts` | MyBatis-Plus 走路由 DataSource |
| GET | `/api/dynamic/accounts?source=REPORT` | 编程式路由 |
| GET | `/actuator/health` | 健康检查 |

## 配置说明

两个库完全独立配置：

```yaml
app:
  datasource:
    master:
      url: jdbc:h2:mem:multi-master...
      hikari:
        pool-name: multi-master-pool
        maximum-pool-size: 10
        minimum-idle: 2
        connection-timeout: 3000
        leak-detection-threshold: 20000
    report:
      url: jdbc:h2:mem:multi-report...
      hikari:
        pool-name: multi-report-pool
        maximum-pool-size: 10
        minimum-idle: 2
        connection-timeout: 3000
        leak-detection-threshold: 20000
```

| 配置 | 说明 |
|---|---|
| `maximum-pool-size` | 每个库独立计算；总连接数是两个池之和 |
| `connection-timeout` | 每个库独立；报表库慢不能拖死主库 |
| `leak-detection-threshold` | 每个池独立告警 |
| `initialization-fail-timeout` | 启动时快速暴露连接问题 |
| `spring.sql.init.mode` | 示例为 `never`，H2 由 `CommandLineRunner` 初始化；生产用迁移工具 |

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 本地 H2，两个库
./mvnw -pl java-ecology-multi-datasource spring-boot:run

# MySQL 主库/报表库
./mvnw -pl java-ecology-multi-datasource clean package
java -jar java-ecology-multi-datasource/target/java-ecology-multi-datasource.jar \
  --spring.profiles.active=mysql
```

MySQL 环境变量：`MYSQL_MASTER_*`、`MYSQL_REPORT_*`，见 `application-mysql.yml`。

## 验证

```bash
# 静态主库：master-alice / master-bob
curl -s http://localhost:8084/api/static/master/accounts

# 静态报表库：report-carol / report-dave / report-eve
curl -s http://localhost:8084/api/static/report/accounts

# 注解动态路由
curl -s http://localhost:8084/api/dynamic/master/accounts
curl -s http://localhost:8084/api/dynamic/report/accounts

# 编程式动态路由
curl -s 'http://localhost:8084/api/dynamic/accounts?source=REPORT'

# 健康检查
curl -s http://localhost:8084/actuator/health
```

## 生产注意事项

- **ThreadLocal 必须清理**：`finally` 中 `clear()`；线程池复用线程时，残留 key 会把下一次请求路由到错误的库。
- **异步/线程池会丢上下文**：`@Async`、`CompletableFuture`、MQ 消费线程不会自动继承 `ThreadLocal`；要显式传 key 或用 `TaskDecorator`。
- **路由必须早于事务**：`@Transactional` 开始时就可能获取连接；AOP 顺序必须高于事务拦截器，否则切库不生效。
- **本地事务不能跨库**：一个 `DataSourceTransactionManager` 只管理一个连接；跨库写入需要 `@DSTransactional`（dynamic-datasource）、Seata 或业务补偿，不能假装 `@Transactional` 能解决。
- **读写分离有复制延迟**：写完立即读从库可能读不到；强一致读必须走主库，或按业务容忍度设计。
- **每个库独立连接池**：`master` 和 `report` 的 `maximum-pool-size` 要分别算，不能共用一个上限；总连接数 = 各池之和。
- **默认库要明确**：`setDefaultTargetDataSource(master)` 是兜底；未设置上下文时走主库，避免误写报表库。
- **静态多数据源不是动态路由**：静态方式适合固定库；动态方式适合按租户/读请求/业务开关切换。两者可以共存，但不要让路由规则散落在 Controller。
- **MP 支持多数据源，但不等于自带动态路由**：MP 能消费任意 `DataSource`；动态切库仍由路由层（`AbstractRoutingDataSource` 或 `dynamic-datasource`）完成。静态多库用多个 `SqlSessionFactory`，动态多库让 MP 使用 routing `DataSource`。
- **自调用不触发 AOP**：同一个 Service 内 `this.reportMethod()` 不会经过代理，`@DataSource` 不生效；要拆到另一个 Bean 或显式设置上下文。
- **生产不要用 `CommandLineRunner` 建表**：本模块只为 H2 演示；真实环境用 Flyway/Liquibase，并让 DBA 审核 DDL。

## 参考

- [Spring `AbstractRoutingDataSource`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/lookup/AbstractRoutingDataSource.html)
- [Spring JDBC](https://docs.spring.io/spring-framework/reference/data-access/jdbc.html)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
- 本项目根 README 的统一模块约定
