# java-ecology-arthas

Arthas 诊断模块：可 attach 的 Spring Boot 目标应用 + 慢调用/热点/异常/嵌套调用场景，用于 `thread`、`trace`、`watch`、`jad`、`profiler` 等命令实战。

## 定位

Arthas 本身是**外部诊断工具**，不是业务依赖。本模块提供两件事：

1. 一个可被 Arthas attach 的 Spring Boot 目标进程；
2. 故意制造的诊断场景，方便验证命令。

支持两种接入：

- **外部 attach（推荐生产）**：`as.sh` / `arthas-boot.jar` 选择目标 JVM
- **Spring Boot Starter 自 attach（开发/预发）**：`spring.arthas.enabled=true`

不包含：Arthas Tunnel Server、Web Console 集群、生产 APM 替代、在线热修复审批。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web、Actuator |
| Arthas 4.3.4 | agent + spring-boot-starter |
| JUnit 5 | 业务与上下文测试 |

## 目录结构

```text
java-ecology-arthas
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/arthas
   │  ├─ ArthasApplication.java
   │  ├─ service/DiagnosticService.java
   │  └─ controller/DiagnosticController.java
   ├─ main/resources/application.yml
   └─ test/java/...（场景 + 上下文）
```

## 演示接口

| 方法 | 路径 | 场景 |
|---|---|---|
| GET | `/api/arthas/slow?millis=300` | 慢调用 |
| GET | `/api/arthas/hot?iterations=1000000` | CPU 热点 |
| GET | `/api/arthas/fail?message=boom` | 异常 |
| GET | `/api/arthas/nested?value=abc` | 嵌套调用 |

`DiagnosticService`：

- `slow`：`LockSupport.parkNanos`，不占 CPU
- `hotLoop`：CPU 循环
- `fail`：抛 `IllegalStateException`
- `nested → reverse`：用于 `trace`/`stack`

## Attach 方式一：外部 attach（推荐）

```bash
# 启动应用
export JAVA_HOME=/env/javaEnv/jdk17
./mvnw -pl java-ecology-arthas spring-boot:run

# 找到 pid
jps -l
# attach
java -jar arthas-boot.jar <pid>
```

或：

```bash
as.sh <pid>
```

## Attach 方式二：Spring Boot Starter

```yaml
spring:
  arthas:
    enabled: true

arthas:
  app-name: java-ecology-arthas
  ip: 127.0.0.1
  telnet-port: 3658
  http-port: 8563
  username: ${ARTHAS_USERNAME:}
  password: ${ARTHAS_PASSWORD:}
  home: ${ARTHAS_HOME:}
  tunnel-server: ${ARTHAS_TUNNEL_SERVER:}
  agent-id: ${ARTHAS_AGENT_ID:}
  session-timeout: 1800
  disabled-commands: stop
```

注意：`spring.arthas.enabled` 只是开关；其余参数属于 `arthas.*`，不是 `spring.arthas.*`。

启动后连接：

```bash
telnet 127.0.0.1 3658
# 或浏览器打开 http://127.0.0.1:8563
```

生产更推荐外部 attach + Tunnel Server，不要长期暴露 telnet/http 端口。

## 常用命令

```bash
# 全局概览
dashboard

# 最忙线程 / 阻塞线程
thread -n 5
thread -b

# 慢调用链路
trace com.loveeleve.ecology.arthas.service.DiagnosticService slow
trace com.loveeleve.ecology.arthas.service.DiagnosticService nested

# 观察入参/返回值/异常
watch com.loveeleve.ecology.arthas.service.DiagnosticService fail \
  '{params, returnObj, throwExp}' -x 2

# 调用栈
stack com.loveeleve.ecology.arthas.service.DiagnosticService hotLoop

# 反编译/热替换排查
jad com.loveeleve.ecology.arthas.service.DiagnosticService
sc -d com.loveeleve.ecology.arthas.service.DiagnosticService

# CPU 采样
profiler start
profiler stop --format html

# 堆/对象
heapdump /tmp/arthas-heap.hprof
vmtool --action getInstances --className java.lang.String
```

## 测试

```bash
./mvnw -pl java-ecology-arthas test
```

当前 3 个测试：

- slow/hot/nested 业务可调用
- fail 抛出预期异常
- Spring 上下文（`spring.arthas.enabled=false`，不 attach）

测试不启动 Arthas agent，不占 3658/8563。

## 生产注意事项

- **不要公网暴露 Telnet/HTTP 端口**：默认只绑 `127.0.0.1`，跨机器用 Tunnel/跳板。
- **必须设认证**：`username/password` 或 Tunnel 鉴权；`stop` 等危险命令默认禁用。
- **按需 attach**：诊断完立即 detach；不要长期挂 agent。
- **`trace`/`watch` 有性能开销**：只对目标类/方法短时间开启，避免全链路放大。
- **`jad`/`redefine` 要审批**：热替换是生产变更，不是普通排查。
- **heapdump 很大**：确认磁盘空间和敏感数据合规。
- **profiler 有采样开销**：高流量时限制时间窗口。
- **Arthas 不是 APM**：链路追踪用 SkyWalking；Arthas 用于现场诊断和命令级观测。

## 未覆盖/未验证

- 未真实 attach/启动 Agent、Tunnel Server、Web Console。
- 未做 `trace/watch/profiler/heapdump` 实机演示。
- 未做热替换、生产权限审批和端口安全加固。

## 参考

- [Arthas](https://arthas.aliyun.com/)
- [Arthas Spring Boot Starter](https://github.com/alibaba/arthas/tree/master/arthas-spring-boot-starter)
- 本项目根 README 的统一模块约定
