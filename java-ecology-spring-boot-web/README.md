# java-ecology-spring-boot-web

Spring Boot + Spring MVC + 内嵌 Tomcat 的生产用法基础模块。后续大部分 Java 生态模块都会复用这里的 Web、配置、异常、日志和运行约定。

## 定位

覆盖研发/生产中的高频与中频场景：

- Spring Boot 启动、Profile、`@ConfigurationProperties`、日志、Actuator
- Spring MVC REST、参数绑定、校验、统一异常、拦截器、Filter、CORS
- 内嵌 Tomcat 的线程/连接/超时/keep-alive/access log/压缩/优雅停机
- 异步 MVC（`CompletableFuture`、`DeferredResult`）、文件上传下载
- 生产配置与开发配置分离

不包含：前端页面、Thymeleaf、JSP、纯展示型 Controller、低价值边角料。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | 自动配置、启动、Actuator |
| Spring MVC | REST、参数/返回值、异常、拦截器 |
| Tomcat | 内嵌容器，`spring-boot-starter-web` 自带，无额外依赖 |
| Bean Validation | Jakarta Validation |
| Actuator | health/info/metrics |
| Logback | 控制台 + 滚动文件 + requestId |

## 目录结构

```text
java-ecology-spring-boot-web
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java/com/loveeleve/ecology/web
   │  │  ├─ WebApplication.java
   │  │  ├─ common/          ApiResponse、RequestIds
   │  │  ├─ config/          DemoProperties、WebMvc、Tomcat、Async、Scheduling
   │  │  ├─ controller/      Hello、User、File、Async、Error
   │  │  ├─ dto/             请求/响应模型
   │  │  ├─ exception/       BusinessException、GlobalExceptionHandler
   │  │  ├─ filter/          RequestIdFilter
   │  │  ├─ interceptor/     RequestLoggingInterceptor
   │  │  ├─ model/           User
   │  │  └─ service/         UserService、AsyncDemoService、FileStorageService、DemoScheduler
   │  └─ resources
   │     ├─ application.yml
   │     ├─ application-dev.yml
   │     ├─ application-prod.yml
   │     └─ logback-spring.xml
   └─ test/java/com/loveeleve/ecology/web/WebApplicationTests.java
```

## 包职责

| 包 | 职责 |
|---|---|
| `common` | 统一响应、requestId 工具 |
| `config` | 属性绑定、MVC、Tomcat、异步、调度配置 |
| `controller` | HTTP 入口，只做参数/编排/响应 |
| `dto` | 请求校验与响应模型 |
| `service` | 业务与存储适配，不依赖 Web |
| `exception` | 业务异常与全局异常映射 |
| `filter` | Servlet Filter（requestId） |
| `interceptor` | Spring MVC 拦截器（handler 日志） |
| `model` | 领域模型 |

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/hello?name=test` | 查询参数、请求头、统一响应 |
| GET | `/api/hello/{name}` | 路径参数 |
| POST | `/api/hello/echo` | JSON 请求体回显 |
| POST | `/api/users` | 创建用户 + 参数校验 |
| GET | `/api/users?page=1&size=20` | 分页列表 |
| GET | `/api/users/{id}` | 查询单个用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |
| POST | `/api/files` | multipart 上传 |
| GET | `/api/files/{storedName}` | 文件下载 |
| GET | `/api/async?input=demo` | `CompletableFuture` 异步 |
| GET | `/api/async/deferred?input=demo` | `DeferredResult` 异步 |
| GET | `/actuator/health` | 健康检查 |
| GET | `/actuator/info` | 应用信息 |
| GET | `/actuator/metrics` | 指标索引 |
| GET | `/error` | 自定义错误响应 |

## 配置说明

### 服务与 Tomcat

| 配置 | 当前值 | 说明 |
|---|---|---|
| `server.port` | `8080` | HTTP 端口 |
| `server.shutdown` | `graceful` | 优雅停机 |
| `server.forward-headers-strategy` | `framework` | 代理后还原 scheme/host |
| `server.max-http-request-header-size` | `16KB` | 请求头上限 |
| `server.tomcat.basedir` | `${TOMCAT_BASEDIR:.}` | Tomcat 工作目录，access log 相对它 |
| `server.tomcat.threads.max` | `200` | worker 线程上限 |
| `server.tomcat.threads.min-spare` | `20` | 最小空闲线程 |
| `server.tomcat.max-connections` | `8192` | 最大连接数 |
| `server.tomcat.accept-count` | `100` | accept backlog |
| `server.tomcat.connection-timeout` | `5s` | 建连/读取请求超时 |
| `server.tomcat.keep-alive-timeout` | `15s` | keep-alive 空闲时间 |
| `server.tomcat.max-keep-alive-requests` | `100` | 单连接最大请求数 |
| `server.tomcat.max-http-form-post-size` | `2MB` | 表单 body 上限 |
| `server.tomcat.max-swallow-size` | `2MB` | 异常时最大吞掉字节 |
| `server.tomcat.uri-encoding` | `UTF-8` | URI 编码 |
| `server.tomcat.accesslog.enabled` | dev `false` / prod `true` | Tomcat access log |
| `server.tomcat.accesslog.directory` | `logs` | 访问日志目录 |
| `server.tomcat.accesslog.pattern` | `%h %l %u %t "%r" %s %b %D` | 访问日志格式 |
| `server.tomcat.accesslog.rotate` | `true` | 按天滚动 |
| `server.tomcat.accesslog.max-days` | `7` | 保留天数 |

### MVC / 上传 / Actuator

| 配置 | 当前值 | 说明 |
|---|---|---|
| `spring.mvc.problemdetails.enabled` | `true` | 支持 ProblemDetail |
| `spring.servlet.multipart.max-file-size` | `10MB` | 单文件上限 |
| `spring.servlet.multipart.max-request-size` | `20MB` | 单请求上限 |
| `management.endpoints.web.exposure.include` | `health,info,metrics` | 暴露端点 |
| `management.endpoint.health.show-details` | dev `always` / prod `never` | 健康细节 |
| `app.demo.request-id-header` | `X-Request-Id` | 请求 ID 头 |
| `app.demo.cors-allowed-origins` | `http://localhost:3000` | CORS 白名单 |
| `app.demo.upload-dir` | `${java.io.tmpdir}/java-ecology-uploads` | 上传目录 |
| `app.demo.async-*` | `4/16/200` | 异步线程池 |

### Profile

| Profile | 用途 | 差异 |
|---|---|---|
| 无 profile（默认） | 生产安全口径 | 错误脱敏、健康最小暴露 |
| `dev` | 本地开发 | 错误详情、健康详情、DEBUG 日志 |
| `prod` | 生产模拟 | 错误脱敏、健康最小暴露、access log、文件日志 |

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 本地开发（显式 dev）
./mvnw -pl java-ecology-spring-boot-web spring-boot:run \
  -Dspring-boot.run.profiles=dev

# 生产/默认（无需 profile）
./mvnw -pl java-ecology-spring-boot-web clean package
java -jar java-ecology-spring-boot-web/target/java-ecology-spring-boot-web.jar
```

## 验证

```bash
curl -s http://localhost:8080/api/hello?name=test
curl -s -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Dave","email":"dave@example.com","age":30}'
curl -s http://localhost:8080/api/users?page=1&size=10
curl -s http://localhost:8080/actuator/health
curl -s -D - http://localhost:8080/api/hello -o /dev/null
```

上传：

```bash
curl -s -F "file=@README.md" http://localhost:8080/api/files
```

## 生产注意事项

- **内嵌 Tomcat 不需要额外依赖**：`spring-boot-starter-web` 已带入 Tomcat；不引入外部 Tomcat，也不使用 JSP。
- **线程数不是越大越好**：`maxThreads` 要结合下游 DB/HTTP 池、CPU、压测和 P99；`max-connections`、`accept-count` 是连接层，不是线程层。
- **超时要分层**：连接超时、keep-alive 空闲、请求处理、下游超时要分开配置，避免重试放大。
- **优雅停机**：`server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase`，发布时先摘流量，再停容器。
- **错误脱敏**：生产不返回 message/stacktrace/binding-errors；统一错误结构由 `ErrorPageController`/`GlobalExceptionHandler` 输出。
- **Actuator 最小暴露**：生产只开必要端点，配合管理端口/网络 ACL/认证；不要裸露 `/actuator`。
- **requestId**：入口 Filter 生成/透传 `X-Request-Id`，写入 MDC 和响应头，日志格式统一带出。
- **CORS 白名单**：按环境配置，不要生产使用 `*` 且同时允许凭证。
- **上传目录**：生产应使用独立挂载盘/对象存储，限制大小与类型，校验文件名，禁止路径穿越。
- **access log**：生产开启 Tomcat access log，按天滚动；应用日志用 Logback 滚动文件，避免只依赖控制台。

## 参考

- [whatsmars](https://github.com/javahongxi/whatsmars) 的模块化组织
- Spring Boot / Spring MVC / Tomcat 官方文档
- 本项目根 README 的统一模块约定
