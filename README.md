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
| `java-ecology-spring-boot-web` | Spring Boot + Spring MVC + 内嵌 Tomcat：REST、校验、异常、拦截器、Filter、异步、上传下载、Actuator、生产配置 | ✅ |
| 后续模块 | Nacos、Dubbo、Redis、Kafka、Sentinel、Seata、ShardingSphere、可观测性等 | ⏳ |

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

## 模块约定

- 包结构按职责隔离：`config`、`controller`、`dto`、`service`、`exception`、`filter`、`interceptor`、`common`。
- 模块 README 必须包含：定位、技术栈、目录结构、接口/能力清单、配置说明、运行验证、生产注意事项。
- 生产配置放在 `application.yml`/`application-prod.yml`，开发覆盖放在 `application-dev.yml`，不把开发便利配置当生产基线。
- 外部依赖后续按“生产集群形态”设计；不在示例里用单机 toy 配置冒充生产方案。

## 说明

当前只完成第一个基础模块，后续按“基础 Web → 数据访问 → 缓存 → 注册/RPC/网关 → 消息 → 稳定性 → 可观测性 → 传输”的顺序逐步补充。
