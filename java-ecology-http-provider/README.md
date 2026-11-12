# java-ecology-http-provider

独立 HTTP Provider，注册到 Nacos，供 OpenFeign Consumer 通过服务名调用。

## 定位

- Spring Boot MVC HTTP 服务
- Nacos 注册中心
- 提供正常、慢调用、404 三个下游接口
- 生产参数：Tomcat 线程/连接/backlog

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web、Actuator |
| Spring Cloud Alibaba 2023.0.1.2 | Nacos Discovery |
| Nacos 2.x | 注册中心 |

## 目录结构

```text
java-ecology-http-provider
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/http/provider
   │  ├─ HttpProviderApplication.java
   │  ├─ controller/DownstreamController.java
   │  └─ dto/EchoResponse.java
   ├─ main/resources/application.yml
   ├─ main/resources/application-test.yml
   └─ test/java/.../ProviderContextTests.java
```

## 功能与文件

| 能力 | 入口/Bean | 对应文件 | 验证 |
|---|---|---|---|
| HTTP 服务 | `DownstreamController` | `controller/DownstreamController.java` | `ProviderContextTests` |
| Nacos 注册 | `spring.cloud.nacos.discovery` | `application.yml` | 代码/配置 |
| 正常调用 | `/api/downstream/hello` | `DownstreamController` | 代码 |
| 慢调用 | `/api/downstream/slow` | `DownstreamController` | 代码 |
| 404 错误 | `/api/downstream/not-found` | `DownstreamController` | 代码 |

## 接口清单

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/downstream/hello?name=` | 正常响应 |
| GET | `/api/downstream/echo?message=` | 透传 traceId |
| GET | `/api/downstream/not-found` | 404 |
| GET | `/api/downstream/slow?millis=` | 慢调用 |
| GET | `/actuator/health` | 健康检查 |

## 配置与运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
export NACOS_SERVER_ADDR=127.0.0.1:8848

java -jar java-ecology-http-provider/target/java-ecology-http-provider.jar
```

服务名：`ecology-http-provider`，端口 `8095`，Nacos group `ECO_ECOLOGY`。

## 生产注意

- 注册中心不是健康检查：配合 Actuator/就绪探针。
- 多实例时 Nacos + LoadBalancer 按实例分发；注意权重/区域。
- 慢接口要独立线程池/超时，不能拖垮整个 Provider。
- 生产启用 Nacos 鉴权/命名空间，不要把 standalone 配置当基线。

## 未覆盖/未验证

- 未接 Nacos 集群/鉴权真实环境；standalone 已手工验证。
- 未做 QPS/并发压测、线程池隔离。
- 未做链路追踪和指标上报。

## 参考

- [Nacos Discovery](https://github.com/alibaba/spring-cloud-alibaba)
- 本项目根 README 的统一模块约定
