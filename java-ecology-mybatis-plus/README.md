# java-ecology-mybatis-plus

MyBatis-Plus 的生产用法模块：`BaseMapper`/`IService`、Lambda Wrapper、分页、乐观锁、逻辑删除、自动填充、枚举映射和防全表更新。

## 定位

MyBatis 解决“SQL 怎么写”，MyBatis-Plus 解决“CRUD 样板代码怎么少写”。本模块覆盖高频与中频能力：

- `BaseMapper<T>`：零 SQL CRUD
- `ServiceImpl<M, T>` / `IService<T>`：业务层通用能力
- `LambdaQueryWrapper` / `LambdaUpdateWrapper`：类型安全条件构造
- 分页插件：`Page<T>` + `PaginationInnerInterceptor`
- 乐观锁：`@Version` + `OptimisticLockerInnerInterceptor`
- 逻辑删除：`@TableLogic`，删除变 UPDATE
- 自动填充：`MetaObjectHandler` 填 `createdAt`/`updatedAt`
- 枚举映射：`@EnumValue` 存 code，Java 侧仍是枚举
- 防全表更新/删除：`BlockAttackInnerInterceptor`
- 自定义 SQL 与 MP 共存：`UserMapper.selectByStatus`

不包含：多数据源、动态数据源、读写分离、分库分表。这些是数据源路由/分片问题，放到独立模块。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | Web、事务、Actuator |
| MyBatis-Plus 3.5.7 | `mybatis-plus-spring-boot3-starter` |
| HikariCP | Spring Boot 默认连接池 |
| H2 | 本地运行与测试，MySQL 兼容模式 |
| MySQL | `mysql` profile 使用 |

## 目录结构

```text
java-ecology-mybatis-plus
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java/com/loveeleve/ecology/mybatisplus
   │  │  ├─ MyBatisPlusApplication.java
   │  │  ├─ common/          ApiResponse
   │  │  ├─ config/          MybatisPlusConfig、AuditMetaObjectHandler
   │  │  ├─ controller/      UserController
   │  │  ├─ dto/             请求、分页、响应
   │  │  ├─ entity/          User（@TableName/@Version/@TableLogic）
   │  │  ├─ enums/           UserStatus（@EnumValue）
   │  │  ├─ exception/       NotFound、OptimisticLock、GlobalExceptionHandler
   │  │  ├─ mapper/          UserMapper（BaseMapper + 自定义 SQL）
   │  │  └─ service/         UserService（ServiceImpl）
   │  └─ resources
   │     ├─ application.yml
   │     ├─ application-dev.yml
   │     ├─ application-mysql.yml
   │     └─ db/              H2/MySQL schema
   └─ test/java/com/loveeleve/ecology/mybatisplus
      ├─ UserServiceTests.java
      └─ UserApiTests.java
```

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/users` | 创建，自动填充审计字段 |
| POST | `/api/users/batch` | 批量创建，事务回滚 |
| GET | `/api/users/{id}` | 按 ID 查询，自动过滤逻辑删除 |
| GET | `/api/users` | Lambda 条件 + 分页 |
| GET | `/api/users/status/{status}` | 自定义 SQL 查询 |
| PUT | `/api/users/{id}` | 更新，带版本号做乐观锁 |
| DELETE | `/api/users/{id}` | 逻辑删除 |
| GET | `/actuator/health` | 健康检查 |

更新请求示例：

```json
{"age": 31, "version": 0}
```

`version` 不传时仍由数据库版本列兜底；并发更新失败返回 `409 OPTIMISTIC_LOCK_CONFLICT`。

## 实体映射

```java
@TableName("ecology_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private UserStatus status;          // @EnumValue 存 int
    @Version
    private Integer version;
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

## 配置说明

| 配置 | 值 | 说明 |
|---|---|---|
| `mybatis-plus.configuration.default-enum-type-handler` | `MybatisEnumTypeHandler` | 支持 `@EnumValue` |
| `mybatis-plus.global-config.db-config.logic-delete-field` | `deleted` | 全局逻辑删除字段 |
| `logic-delete-value` / `logic-not-delete-value` | `1` / `0` | 删除标记 |
| `id-type` | `auto` | 数据库自增 |
| `PaginationInnerInterceptor` | `DbType.MYSQL` | 分页方言 |
| `OptimisticLockerInnerInterceptor` | 启用 | `@Version` 生效 |
| `BlockAttackInnerInterceptor` | 启用 | 阻止无 WHERE 的 update/delete |
| Hikari `maximum-pool-size` | `20` | 连接池上限 |

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 本地 H2
./mvnw -pl java-ecology-mybatis-plus spring-boot:run

# 本地 H2 + SQL DEBUG
./mvnw -pl java-ecology-mybatis-plus spring-boot:run \
  -Dspring-boot.run.profiles=dev

# MySQL
./mvnw -pl java-ecology-mybatis-plus clean package
java -jar java-ecology-mybatis-plus/target/java-ecology-mybatis-plus.jar \
  --spring.profiles.active=mysql
```

## 验证

```bash
# 创建
curl -s -X POST http://localhost:8083/api/users \
  -H 'Content-Type: application/json' \
  -d '{"username":"plus-user","email":"plus@example.com","age":30}'

# 分页 + Lambda 条件
curl -s 'http://localhost:8083/api/users?username=plus&status=ACTIVE&page=1&size=10'

# 自定义 SQL
curl -s http://localhost:8083/api/users/status/ACTIVE

# 乐观锁更新：第一次 version=0，第二次必须用新 version
curl -s -X PUT http://localhost:8083/api/users/1 \
  -H 'Content-Type: application/json' \
  -d '{"age":31,"version":0}'

# 逻辑删除
curl -s -X DELETE http://localhost:8083/api/users/1

# 健康检查
curl -s http://localhost:8083/actuator/health
```

## 生产注意事项

- **逻辑删除不是物理删除**：唯一索引仍然会挡住重复 username；需要“删除后重建同名”时，把唯一键设计成 `(username, deleted)` 或加删除时间戳，不能假设删除后唯一约束消失。
- **自定义 SQL 不受 `@TableLogic` 自动过滤**：`selectByStatus` 必须自己写 `deleted = 0`；否则会把已删除数据查出来。
- **乐观锁只保护 `updateById` 这类带版本的更新**：自定义 UPDATE 或 `LambdaUpdateWrapper` 不会自动加版本条件，需要自己处理。
- **`BlockAttackInnerInterceptor` 是兜底**：它拦截无 WHERE 的 update/delete，不要把它当权限/租户隔离。
- **自动填充不是数据库默认值**：`MetaObjectHandler` 只在 MP 的 insert/update 链路生效，自定义 SQL、批量 JDBC 需要自己赋值。
- **批量写入要控制大小**：`saveBatch` 默认分批，但每批仍受 `max_allowed_packet`、事务大小、锁持有时间限制。
- **分页插件要限制最大页大小**：本模块在 service 层把 `size` 限制到 100；生产还应对深分页做游标/延迟关联优化。
- **枚举存 code 不存 name**：`@EnumValue` 让数据库存 `1/0`，避免改枚举常量名导致数据失效；新增枚举值要保证 code 不复用。
- **多数据源不要塞进本模块**：数据源路由、读写分离、分库分表是独立关注点，见后续 `java-ecology-multi-datasource`。

## 参考

- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [MyBatis-Plus 插件](https://baomidou.com/plugins/)
- [MyBatis 官方文档](https://mybatis.org/mybatis-3/)
- 本项目根 README 的统一模块约定
