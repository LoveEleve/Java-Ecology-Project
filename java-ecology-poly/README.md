# Poly

> Five independent Spring projects: multi-zone routing, unified configuration, dynamic datasources, Redis multi-datacenter replication and endpoint-level gateway routing.
> 五个独立的 Spring 项目：多活区域路由、配置中心统一抽象、动态多数据源、Redis 多机房复制、端点级网关路由。

## 定位

完整 vendoring 的 Poly 项目：保留 5 个子项目、源码、测试、Maven Wrapper、LICENSE/NOTICE，不是 Maven 外部依赖。

## 项目一览

| 项目 | 定位 | 基线 | 测试 |
|------|------|------|------|
| [polyzone](./polyzone) | 区域感知路由：四级区域定位 → 同区域优先过滤（三级安全阀）→ 事件联动 → 配置中心动态切换 | Spring Boot 3.2 / Spring Cloud 2023.0.3 | 132 |
| [polyconfig](./polyconfig) | 配置中心统一抽象：声明式 `@PropertySource` 扩展 + 属性级变更事件（名 + 新旧值 + 来源）+ 后端适配器模板 | Spring Boot 3.2 | 24 |
| [polydata](./polydata) | 动态多数据源与分库分表：子上下文隔离、热替换、同区域优先/故障转移、ORM 同进程并存、ShardingSphere YAML 直构 | Spring Boot 3.2 / ShardingSphere 5.5.1 | 80 |
| [polycache](./polycache) | Redis 多机房复制：Redisson 命令级无侵入拦截 → 写命令过滤 → Kafka 分区 → 语义重放（防回环 / 至少一次 / 分区内保序），含动态连接路由与拦截器 SPI | Spring Boot 3.2 / Redisson 3.27 / Kafka | 60 |
| [polygateway](./polygateway) | 端点级网关路由（Spring Cloud Gateway WebFlux）：服务发布端点元数据，网关生成 `lb://` 精确路由（官方谓词 + 响应式负载均衡，零阻塞） | Spring Cloud Gateway 4.1 | 39 |

合计 **335 个测试**（上游 README 统计；本仓库实际构建测试报告以根 Reactor 为准）。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译基线 |
| Spring Boot 3.2.5 | 各子项目运行基线 |
| Spring Cloud 2023.0.3 | PolyZone/PolyGateway |
| ShardingSphere 5.5.1 | PolyData |
| Redisson 3.27.0 / Kafka | PolyCache |
| Maven Wrapper | 各子项目自带 |

## 目录结构

```text
java-ecology-poly
├─ polyzone/       区域路由与多活
├─ polyconfig/     配置中心抽象
├─ polydata/       动态多数据源
├─ polycache/      Redis 多机房复制
├─ polygateway/    端点级网关路由
├─ VENDORED.md     来源/上游 commit/拷贝边界
├─ LICENSE
└─ NOTICE
```

## 功能与文件

| 子项目 | 入口 | 说明 | 测试 |
|---|---|---|---|
| PolyZone | `polyzone/pom.xml` | 区域上下文、区域优先、Spring/Cloud/AWS 适配 | 自有 `src/test` |
| PolyConfig | `polyconfig/pom.xml` | 配置中心抽象、变更事件、Nacos 适配 | 自有 `src/test` |
| PolyData | `polydata/polydata-jdbc-spring-boot` | 动态多数据源、热替换、HA、ORM/SS 集成 | 93 个用例 |
| PolyCache | `polycache/pom.xml` | Redis 多数据中心复制、Redisson 捕获/回放 | 自有 `src/test` |
| PolyGateway | `polygateway/pom.xml` | 端点级网关路由、服务实例元数据 | 自有 `src/test` |

完整源码和许可证见各子目录；来源、上游 commit 和拷贝边界见 `VENDORED.md`。本目录不是外部依赖，根 Reactor 会直接构建这里的所有子项目。

## 接口清单

本目录是聚合项目，没有统一 HTTP 入口；各子项目接口见其自身 README。

## 构建

```bash
# 聚合构建全部（按依赖顺序，polydata 的 SNAPSHOT 依赖由同一 reactor 解析）
mvn clean test

# 单独构建某个项目
mvn -f polyzone/pom.xml clean test

# polydata 依赖 polyzone 与 polyconfig 的 1.0.0-SNAPSHOT，单独构建前先安装两者
mvn -f polyzone/pom.xml clean install
mvn -f polyconfig/pom.xml clean install
mvn -f polydata/pom.xml clean test
```

每个子目录都是自包含的多模块工程（自己的父 pom、README、LICENSE、NOTICE、`mvnw`），可以单独 clone、单独使用。

## License

Apache License, Version 2.0，详见各子目录的 `LICENSE` 与 `NOTICE`。

## 未覆盖/未验证

- 本目录是上游 Poly 项目的完整源码快照，不是重新分析或重写版本。
- 上游 README 的测试计数可能与当前快照不同，以根 Reactor 的实际测试报告为准。
- 子项目的外部依赖（Kafka、真实 Redis 集群、MySQL 等）环境未全部重建。
