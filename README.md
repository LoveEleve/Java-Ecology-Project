# Java-Ecology-Project

Java 生态的生产级使用案例集合，基于 Spring Boot 构建。目标不是堆零散 Demo，而是给每个中间件/框架提供**可运行、可验证、可复制**的标准用法，重点覆盖研发与生产环境中的高频、中频场景。

## 项目原则

- **一个组件一个模块**：模块之间包和依赖隔离，避免示例互相污染。
- **生产视角**：配置、超时、线程、连接、日志、错误处理、优雅停机、可观测性按生产口径组织。
- **不依赖 Docker**：示例本身不绑定容器编排；需要外部服务时，在对应模块 README 中说明本地/集群接入方式。
- **统一版本管理**：Spring Boot Parent + Maven Wrapper + `dependencyManagement`，模块不散落版本号。
- **只保留有价值场景**：核心用法必须覆盖；高频/中频生产用法补充；前端、边角料、纯展示型代码不做。
- **参考官方与成熟实践**：模块结构参考 [whatsmars](https://github.com/javahongxi/whatsmars)，具体用法以 Spring 官方文档/仓库为准。

## 技术基线

| 项 | 版本 |
|---|---|
| JDK | 17 |
| Spring Boot | 3.2.5 |
| 构建 | Maven 3.9+（含 Maven Wrapper） |
| 包管理 | 父 POM + BOM + `dependencyManagement` |
| 运行方式 | `java -jar` / `spring-boot:run` |

## 模块

| 模块 | 说明 | 状态 |
|---|---|---|
| `java-ecology-spring-boot-extension` | Spring Boot 扩展点：自动配置、条件、EnvironmentPostProcessor、RunListener、FailureAnalyzer、Import、BFPP/BPP、Actuator、配置绑定 | ✅ |
| `java-ecology-spring-boot-web` | Spring Boot + Spring MVC + 内嵌 Tomcat：REST、校验、异常、拦截器、Filter、异步、上传下载、Actuator、生产配置 | ✅ |
| `java-ecology-mybatis` | MyBatis：XML/注解 Mapper、动态 SQL、TypeHandler、批量、事务、分页、慢 SQL、HikariCP | ✅ |
| `java-ecology-druid` | Druid：连接池、StatView、WebStat、WallFilter、慢 SQL、池状态 | ✅ |
| `java-ecology-mybatis-plus` | MyBatis-Plus：BaseMapper/IService、Lambda Wrapper、分页、乐观锁、逻辑删除、自动填充 | ✅ |
| `java-ecology-multi-datasource` | 多数据源：静态多库、`AbstractRoutingDataSource`、`@DataSource` AOP、MP 接入、连接池隔离 | ✅ |
| `java-ecology-poly` | 完整 vendoring Poly 项目：PolyZone/PolyConfig/PolyData/PolyCache/PolyGateway 源码与测试 | ✅ |
| `java-ecology-redis` | Redis：Spring Data Redis、Lettuce、Cache、Lua、Pipeline、Cluster 配置 | ✅ |
| `java-ecology-redisson` | Redisson：分布式锁/看门狗、公平锁/读写锁、限流、信号量、MapCache | ✅ |
| `java-ecology-nacos` | Nacos：配置中心、动态刷新、服务注册与发现 | ✅ |
| `java-ecology-sharding-jdbc` | ShardingSphere-JDBC：2 库 × 2 表、分片键/主键不一致、Binding、Hint、读写分离、影子库 | ✅ |
| `java-ecology-dubbo` | Dubbo：API/Provider/Consumer 三模块、版本/分组、超时/重试、泛化、异步、Nacos | ✅ |
| `java-ecology-lucene` | Lucene：嵌入式索引、SmartCN、QueryParser、BM25、高亮、facet、NRT | ✅ |
| `java-ecology-elasticsearch` | Elasticsearch：Java Client、mapping、CRUD、bulk、search_after、高亮、聚合 | ✅ |
| `java-ecology-http-provider` | 独立 HTTP Provider：MVC、Nacos 注册、慢调用/404 测试接口 | ✅ |
| `java-ecology-openfeign` | OpenFeign：Nacos + LB、Sentinel、HC5 池、超时/重试/降级 | ✅ |
| `java-ecology-gateway` | Gateway：Nacos + LB、Sentinel 流控、Redis RateLimiter、CORS、TraceId | ✅ |
| `java-ecology-kafka` | Kafka：幂等 Producer、批量消费、手动 ack、DB 幂等、重试、DLT | ✅ |
| `java-ecology-rocketmq` | RocketMQ：同步/异步/单向/批量/延迟/顺序/事务消息、并发、幂等、DLQ | ✅ |
| `java-ecology-sentinel` | Sentinel：流控、熔断、热点参数、系统/授权规则、注解降级、Nacos 动态规则 | ✅ |
| `java-ecology-seata` | Seata AT：Order/Account/Storage 三服务、全局事务、XID 透传、幂等、undo_log、Nacos 集群配置 | ✅ |
| `java-ecology-observability` | 可观测性：Micrometer/Prometheus、OpenTelemetry Tracing、TraceId 日志、Health/Info | ✅ |
| `java-ecology-netty` | Netty：TCP Server/Client、长度字段拆包、心跳、背压、重连、优雅停机 | ✅ |
| 后续模块 | 传输扩展等 | ⏳ |

## 构建与运行

```bash
# JDK 17
export JAVA_HOME=/env/javaEnv/jdk17

# 全量构建（含测试）
./mvnw clean verify

# 只构建当前 Web 模块
./mvnw -pl java-ecology-spring-boot-web -am clean package

# 运行
./mvnw -pl java-ecology-spring-boot-web spring-boot:run
# 或
java -jar java-ecology-spring-boot-web/target/java-ecology-spring-boot-web.jar
```

默认不激活 profile，配置按生产安全口径；本地开发显式启用 `dev`：

```bash
# 默认（生产安全口径）
java -jar java-ecology-spring-boot-web/target/java-ecology-spring-boot-web.jar

# 本地开发
java -jar java-ecology-spring-boot-web/target/java-ecology-spring-boot-web.jar --spring.profiles.active=dev
```

## 覆盖边界（代码 vs 验证）

> 这张表区分“代码已写”“测试已覆盖”“只有 README 方案”，避免把未实现能力说成已验证。

| 模块/能力 | 代码 | 测试 | 说明 |
|---|---|---|---|
| Sharding 2 库 × 2 表 | ✅ | ✅ H2 | `ShardingOrderTests` |
| Sharding 分片键/主键不一致 | ✅ | ✅ H2 | `t_order_item` 的 PK `item_id`、分片键 `order_id` |
| Sharding Binding Table | ✅ | ✅ H2 | `t_order` + `t_order_item` 关联查询 |
| Sharding Hint | ✅ | ✅ H2 | 自定义 HINT 算法、`t_order_hint` |
| Sharding 读写分离 | ✅ | ✅ H2 | 4 个 H2 库，写 master/读 slave |
| Sharding 影子库 `!SHADOW` | ✅ | ✅ H2 | `shadow` profile、`!SINGLE` + `!SHADOW` |
| Sharding MySQL 读写分离/影子库 | ✅ 配置代码 | ❌ 未连真实 MySQL | `application-*-mysql.yml` |
| Sharding 扩容/影子表迁移 | ✅ 计划代码 + README 方案 | ⚠️ 计划已测 | `ShardExpansionService` 只生成迁移计划，不执行迁移 |
| Sharding 分布式事务/加密脱敏 | ❌ | ❌ | Seata/加密规则后续模块 |
| MyBatis 二级缓存/Provider SQL/association/collection/BatchExecutor | ✅ 已写示例 | ⚠️ Provider/Batch 已测；Cache/association 未测 | `UserCacheMapper`、`UserProvider`、`UserWithOrdersMapper`、`UserBatchService` |
| MyBatis-Plus 多租户/数据权限/动态表名/SQL 注入器 | ✅ 已写示例 | ⚠️ SQL 注入器已测；其余默认关闭 | `AdvancedMybatisPlusProperties` + 扩展拦截器/注入器 |
| 多数据源静态/动态/MP/事务 | ✅ | ✅ H2 | 读写分离不在本模块 |
| Redis Cache/Lua/Pipeline/结构 | ✅ | ✅ 临时 Redis | 核心已测 |
| Redis Pub/Sub/事务 | ✅ 已写示例 | ⚠️ 事务已测；Pub/Sub 未测 | `RedisPubSubConfig`、`RedisTransactionService` |
| Redis Cluster | ⚠️ 仅 YAML | ❌ | `application-cluster.yml`，无真实集群验证 |
| Redisson 锁/限流/信号量/MapCache | ✅ | ✅ 临时 Redis | 核心已测 |
| Redisson RedLock/异步 | ✅ 已写示例 | ⚠️ 单客户端 API 已测 | 不是多主 RedLock |
| Redisson Cluster | ⚠️ 仅 YAML | ❌ | `application-cluster.yml`，无真实集群验证 |
| Nacos 配置/发现/刷新 | ✅ | ✅ 关闭模式 + 手工 Nacos 2.3.2 | 测试不自动启动 Nacos |
| Dubbo API/Provider/Consumer | ✅ 三模块拆分 | ⚠️ Provider 测试；消费者需 Nacos | 独立端口 8090/8091 |
| Dubbo 泛化/异步/Attachment | ✅ | ⚠️ Nacos 手工验证 | Callback 已移除，暂不展开 |
| Dubbo Filter/参数校验 | ✅ 代码/配置 | ❌ 未做故障注入测试 | Mock 暂不启用 |
| Lucene 索引/查询/高亮/facet/NRT | ✅ | ✅ 内存索引测试 | FSDirectory 未做持久化测试 |
| Elasticsearch Client/CRUD/bulk/search_after/聚合 | ✅ | ⚠️ 本地 ES 手工验证 | 自动化只测 Bean |
| OpenFeign + Nacos/LB + Sentinel + HC5 | ✅ | ✅ Nacos + Provider + Consumer 手工验证 | 自动化只测 Fallback |
| Gateway + Nacos/LB + Sentinel + Redis 限流 | ✅ | ✅ Nacos + Redis + Provider 手工验证 | 自动化只测上下文 |
| Kafka Producer/Consumer/Batch/DB 幂等/DLT | ✅ | ✅ Embedded Kafka | 未接真实集群 |
| RocketMQ 同步/异步/延迟/顺序/事务 | ✅ | ✅ 12 个单测（无 Broker） | 未接真实 Broker |
| Sentinel 流控/熔断/热点/系统/授权 | ✅ | ✅ 16 个单测（无 Dashboard/Nacos） | 未接真实规则中心 |
| Seata AT 全局事务/三服务/幂等/undo_log | ✅ | ✅ 26 个单测/上下文（无 TC/Nacos/MySQL） | 未接真实 TC/MySQL |
| Observability 指标/Tracing/Health | ✅ | ✅ 4 个测试（无 Collector/Prometheus） | 未接真实监控后端 |
| Netty TCP 拆包/编解码/Handler/重连 | ✅ | ✅ 12 个测试（含 loopback） | 未做 TLS/压测 |
| Nacos 集群/鉴权 | ⚠️ 配置支持 | ❌ | 仅 standalone 手工验证 |
| Druid 池/StatView/WallFilter | ✅ 配置/代码 | ⚠️ H2 池配置测试；WallFilter 仅 MySQL 配置 | WallFilter/StatView 未在真实 MySQL 验证 |
| Poly 全家桶 | ✅ vendored | ✅ 359 个测试 | 原项目源码整体拷贝 |

环境不允许的能力保留配置和 README 说明，不把它们标成“已验证”。

## 模块 README 标准

每个模块 README 统一包含：

1. **定位/边界**：这个模块做什么，不做什么。
2. **技术栈**：关键组件与版本。
3. **目录结构**：主要 package 与资源文件。
4. **功能与文件**：能力 → 入口/Bean → 对应文件 → 验证状态。
5. **接口清单**：HTTP/Actuator/CLI 入口。
6. **配置说明**：本地、生产 profile、关键参数。
7. **运行与验证**：命令、curl/测试方式。
8. **生产注意事项**：超时、容量、一致性、失败模式。
9. **未覆盖/未验证**：明确写“代码未写”“仅配置”“环境未验证”，不混为一谈。

## 模块约定

- 包结构按职责隔离：`config`、`controller`、`dto`、`service`、`exception`、`filter`、`interceptor`、`common`。
- 模块 README 必须包含：定位、技术栈、目录结构、接口/能力清单、配置说明、运行验证、生产注意事项。
- 生产配置放在 `application.yml`/`application-prod.yml`，开发覆盖放在 `application-dev.yml`，不把开发便利配置当生产基线。
- 外部依赖后续按“生产集群形态”设计；不在示例里用单机 toy 配置冒充生产方案。

## 说明

当前已完成基础 Web、数据访问、连接池、ORM 增强、多数据源、Redis、Redisson、Nacos、ShardingSphere-JDBC、Dubbo、Lucene、Elasticsearch、OpenFeign、Gateway、Kafka、RocketMQ、Sentinel、Seata、Observability 和 Netty 模块，并完整 vendoring 了 Poly 项目；后续按“稳定性 → 可观测性 → 传输”的顺序逐步补充。
