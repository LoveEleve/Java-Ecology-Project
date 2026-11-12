# java-ecology-nacos

Nacos 生产用法模块：配置中心、动态刷新、服务注册和服务发现。默认不连接 Nacos，保证本地可启动；`nacos` profile 接入真实 Nacos。

## 定位

覆盖研发/生产中的核心场景：

- 配置中心：`spring.config.import` 拉取 `dataId/group` 配置
- 动态刷新：Nacos 配置变更后，`@ConfigurationProperties` 自动 rebind
- 配置发布：`NacosConfigManager` 发布配置并读取
- 服务注册：应用启动后注册到 Nacos
- 服务发现：`DiscoveryClient` 查询服务列表和实例
- 命名空间/分组：`namespace` 隔离环境，`group` 隔离业务
- 本地默认关闭，`nacos` profile 显式开启

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译与运行基线 |
| Spring Boot 3.2.5 | Web、Actuator、配置绑定 |
| Spring Cloud 2023.0.3 | DiscoveryClient、配置导入 |
| Spring Cloud Alibaba 2023.0.1.2 | Nacos Config + Discovery starter |
| Nacos 2.3.2 | 本地/集群 Nacos Server |

## 目录结构

```text
java-ecology-nacos
├─ pom.xml
├─ README.md
└─ src
   ├─ main
   │  ├─ java/com/loveeleve/ecology/nacos
   │  │  ├─ NacosApplication.java
   │  │  ├─ common/          ApiResponse
   │  │  ├─ config/          NacosDemoProperties
   │  │  ├─ controller/      NacosController
   │  │  ├─ dto/             PublishConfigRequest
   │  │  └─ exception/       GlobalExceptionHandler
   │  └─ resources
   │     ├─ application.yml
   │     ├─ application-nacos.yml
   │     └─ application-dev.yml
   └─ test/java/com/loveeleve/ecology/nacos
      └─ NacosControllerTests.java
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| 配置导入/刷新 | `spring.config.import` + `NacosDemoProperties` | `application-nacos.yml`、`config/NacosDemoProperties.java` | 真实 Nacos 手工验证 |
| 配置读取/发布 | `NacosController` | `controller/NacosController.java` | `NacosControllerTests`（关闭模式）+ 手工验证 |
| 服务注册 | Nacos Discovery starter | `application-nacos.yml` | 真实 Nacos 手工验证 |
| 服务发现 | `DiscoveryClient` | `controller/NacosController.java` | `NacosControllerTests` + 手工验证 |
| namespace/group/auth | Nacos properties | `application-nacos.yml` | standalone 手工验证 |

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/nacos/config` | 当前本地配置（Nacos 导入后会被刷新） |
| GET | `/api/nacos/remote-config?dataId=&group=` | 直接读取 Nacos 配置 |
| POST | `/api/nacos/config` | 发布 Nacos 配置（默认关闭写入口） |
| GET | `/api/nacos/services` | 查询服务列表 |
| GET | `/api/nacos/instances?serviceId=` | 查询服务实例 |
| GET | `/actuator/health` | 健康检查 |

## 配置说明

### 默认：不连 Nacos

```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
```

应用可以单独启动，配置接口返回本地值，服务列表为空。适合没有 Nacos 的本地开发。

### `nacos` profile：连接真实 Nacos

```bash
export NACOS_SERVER_ADDR=127.0.0.1:8848
export NACOS_USERNAME=nacos
export NACOS_PASSWORD=nacos
export NACOS_NAMESPACE=
export NACOS_PUBLISH_ENABLED=true

java -jar java-ecology-nacos/target/java-ecology-nacos.jar \
  --spring.profiles.active=nacos
```

关键配置：

```yaml
spring:
  config:
    import: optional:nacos:${spring.application.name}.yaml?group=ECO_ECOLOGY&refreshEnabled=true
  cloud:
    nacos:
      server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD:nacos}
      discovery:
        enabled: true
        namespace: ${NACOS_NAMESPACE:}
        group: ECO_ECOLOGY
      config:
        enabled: true
        namespace: ${NACOS_NAMESPACE:}
        group: ECO_ECOLOGY
        file-extension: yaml
        refresh-enabled: true

ecology:
  nacos:
    publish-enabled: ${NACOS_PUBLISH_ENABLED:false}
```

`publish-enabled` 默认 `false`：读接口可以开放，但写配置必须显式开启。生产环境还应加鉴权/网关 ACL，不能只靠这个开关。

**`public` 命名空间用空字符串**。不要把默认命名空间写成 `public`，否则可能读到不同的 tenant，表现为“发布成功但读取为 null”。

### 配置中心

- dataId：`java-ecology-nacos.yaml`
- group：`ECO_ECOLOGY`
- namespace：默认 public（空字符串）
- 格式：YAML
- 刷新：`refreshEnabled=true`

本地配置中的 `ecology.nacos.message` 会被远端同名属性覆盖；发布后无需重启，`@ConfigurationProperties` 会 rebind。

### 服务注册与发现

注册信息：

```text
serviceId = spring.application.name = java-ecology-nacos
group     = ECO_ECOLOGY
namespace = public
```

`GET /api/nacos/instances?serviceId=java-ecology-nacos` 返回 host/port/uri；实例默认临时节点，客户端心跳维持。

## 运行

### 启动本地 Nacos

```bash
# 下载 Nacos 2.3.2 后
cd nacos
bash bin/startup.sh -m standalone

# 控制台
open http://127.0.0.1:8848/nacos
```

默认账号 `nacos/nacos`，生产必须改密码并配置鉴权。

### 启动应用

```bash
export JAVA_HOME=/env/javaEnv/jdk17

# 不连 Nacos
./mvnw -pl java-ecology-nacos spring-boot:run

# 连 Nacos
./mvnw -pl java-ecology-nacos spring-boot:run \
  -Dspring-boot.run.profiles=nacos
```

## 验证

```bash
# 本地配置
curl -s http://localhost:8087/api/nacos/config

# 发布配置
curl -s -X POST http://localhost:8087/api/nacos/config \
  -H 'Content-Type: application/json' \
  -d '{"dataId":"java-ecology-nacos.yaml","group":"ECO_ECOLOGY","content":"ecology:\n  nacos:\n    message: remote-hello\n","type":"yaml"}'

# 读取远端
curl -s 'http://localhost:8087/api/nacos/remote-config?dataId=java-ecology-nacos.yaml&group=ECO_ECOLOGY'

# 等一下再查本地配置，应已刷新为 remote-hello
curl -s http://localhost:8087/api/nacos/config

# 服务发现
curl -s http://localhost:8087/api/nacos/services
curl -s 'http://localhost:8087/api/nacos/instances?serviceId=java-ecology-nacos'

# 健康检查
curl -s http://localhost:8087/actuator/health
```

本机已用 Nacos 2.3.2 standalone 实测：配置发布、远端读取、本地刷新、服务注册、实例发现均正常。仓库测试只覆盖“Nacos 关闭”模式，不依赖外部 Nacos。

## 生产注意事项

- **命名空间不要写 `public`**：默认 public 的 namespace ID 是空字符串；写成 `public` 可能发布/读取不在同一个 tenant。
- **配置写入口默认关闭**：`ecology.nacos.publish-enabled=false`；只有明确需要发布配置的运维/管理端才开启，并对接口做鉴权。
- **dataId/group 要统一约定**：应用名、环境、租户应体现在 namespace/group/dataId，避免不同环境互相覆盖。
- **配置刷新不是事务**：Nacos 推送后 Bean rebind 是最终一致；强一致配置要在启动时校验并 fail-fast。
- **配置内容要可回滚**：发布前备份，使用灰度/分组；不要把密钥明文写进普通配置，配合 KMS/加密配置。
- **服务注册不是健康检查**：实例注册成功不代表业务健康；生产要配合 Actuator、就绪探针和网关摘流。
- **临时实例会随心跳消失**：默认 ephemeral；长连接断开后实例会被剔除，客户端要有重试/负载均衡降级。
- **集群不是单机**：生产 Nacos 是集群 + 外部 MySQL（或 Derby 仅单机），需要 VIP/SLB、鉴权、备份和容量规划。
- **namespace/group 是隔离边界，不是权限边界**：多租户隔离还要配合 Nacos 鉴权和网络 ACL。
- **配置监听要防惊群**：大量实例同时刷新会放大下游压力；刷新逻辑要幂等、限流、可观测。
- **不要把 Nacos 当业务数据库**：配置中心适合小配置，不适合高频写和大对象。

## 未覆盖/未验证

- Nacos 集群、鉴权、TLS、命名空间隔离只做了 standalone 手工验证。
- 自动化测试只覆盖 Nacos 关闭模式，不启动外部 Nacos。
- 配置写入默认关闭，且接口鉴权需外部网关/ACL。

## 参考

- [Nacos 官方文档](https://nacos.io/docs/latest/)
- [Spring Cloud Alibaba Nacos Config](https://github.com/alibaba/spring-cloud-alibaba)
- [Spring Cloud Commons DiscoveryClient](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/discovery-client.html)
- 本项目根 README 的统一模块约定
