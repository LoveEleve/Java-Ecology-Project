# java-ecology-xxl-job

XXL-Job 执行器模块：Simple Job、Sharding Job、失败处理、执行器配置、调度中心接入。

## 定位

覆盖 XXL-Job 执行器生产使用的主链路：

- 执行器注册（admin addresses / appname / accessToken）
- `@XxlJob` Simple Job
- `@XxlJob` Sharding Job
- `XxlJobHelper` 参数、日志、成功/失败
- 分片下标/总分片计算
- 任务失败返回调度中心
- 日志目录/保留天数
- Actuator

不包含：XXL-Job Admin、告警、任务编排、失败重试策略、任务幂等框架、分布式锁。

## 技术栈

| 组件 | 说明 |
|---|---|
| JDK 17 | 编译/运行 |
| Spring Boot 3.2.5 | Web、Actuator |
| XXL-Job 2.4.2 | executor/core |
| JUnit 5 | Job 业务逻辑测试 |

## 目录结构

```text
java-ecology-xxl-job
├─ pom.xml
├─ README.md
└─ src
   ├─ main/java/com/loveeleve/ecology/xxljob
   │  ├─ XxlJobApplication.java
   │  ├─ config/XxlJobProperties.java
   │  ├─ config/XxlJobConfig.java
   │  ├─ job/DemoSimpleJob.java
   │  ├─ job/DemoShardingJob.java
   │  ├─ model/JobResult.java
   │  └─ service/DemoJobService.java
   ├─ main/resources/application.yml
   └─ test/java/...（Job 业务 + 上下文）
```

## 任务

| 任务名 | 类型 | 说明 |
|---|---|---|
| `demoSimpleJob` | Simple | 参数含 `fail` 时返回失败 |
| `demoShardingJob` | Sharding | 按 `shardIndex/shardTotal` 处理 100 条数据的子集 |

分片示例：

- shard 0/3 → 34 条
- shard 1/3 → 33 条
- shard 2/3 → 33 条

## 配置

```yaml
app:
  xxl-job:
    enabled: ${XXL_JOB_ENABLED:false}
    admin-addresses: ${XXL_JOB_ADMIN_ADDRESSES:http://127.0.0.1:8080/xxl-job-admin}
    access-token: ${XXL_JOB_ACCESS_TOKEN:default_token}
    appname: ${XXL_JOB_APPNAME:java-ecology-xxl-job-executor}
    address: ${XXL_JOB_ADDRESS:}
    ip: ${XXL_JOB_IP:}
    port: ${XXL_JOB_PORT:9999}
    log-path: ${XXL_JOB_LOG_PATH:./logs/xxl-job}
    log-retention-days: 30
```

生产必须：

- `XXL_JOB_ENABLED=true`
- `admin-addresses` 指向 Admin 集群
- `access-token` 与 Admin 一致，并走密钥管理
- `appname`、`address` 在多实例下正确注册
- 日志目录挂持久盘并设置保留天数

## 状态接口

`GET /api/xxl-job/status` 可查看执行器配置；`enabled=false` 时不会启动执行器：

```json
{
  "enabled": false,
  "appname": "java-ecology-xxl-job-executor",
  "adminAddresses": "http://127.0.0.1:8080/xxl-job-admin",
  "port": 9999,
  "logPath": "./logs/xxl-job"
}
```

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
export XXL_JOB_ENABLED=true
export XXL_JOB_ADMIN_ADDRESSES=http://xxl-job-admin:8080/xxl-job-admin
./mvnw -pl java-ecology-xxl-job spring-boot:run
```

在 Admin 中新建执行器：

- AppName：`java-ecology-xxl-job-executor`
- 注册方式：自动注册
- 任务：`demoSimpleJob` / `demoShardingJob`

## 测试

```bash
./mvnw -pl java-ecology-xxl-job test
```

当前 5 个测试：

- Simple Job 成功/失败
- Sharding Job 分片数量
- 非法分片拒绝
- Spring 上下文（执行器关闭，不连接 Admin）
- `/api/xxl-job/status` 执行器配置查询

测试不依赖 XXL-Job Admin。

## 生产注意事项

- **任务必须幂等**：调度可能重复触发/重试；用业务唯一键、状态机或分布式锁。
- **分片不是数据切分本身**：`shardIndex/shardTotal` 只是路由提示，数据源/分片规则要自己保证。
- **执行器多实例**：Admin 路由策略、自动注册、地址/端口不能冲突；容器化注意 IP 注册。
- **失败返回**：必须 `handleFail`，否则 Admin 认为成功，不会告警/重试。
- **超时**：任务执行时间要小于调度超时；长任务拆分或异步化。
- **日志**：`XxlJobHelper.log` 写执行器日志，Admin 可查看；日志保留和磁盘容量要监控。
- **accessToken**：不能使用默认值，Admin/Executor 必须一致并保密。
- **告警**：Admin 失败告警、执行器掉线告警、任务超时告警要配置。
- **不要用 XXL-Job 替代 MQ**：它是调度系统，不是高吞吐消息队列。

## 未覆盖/未验证

- 未部署 XXL-Job Admin，未做真实调度触发。
- 未做任务幂等、失败重试、告警、任务编排。
- 未做多执行器自动注册/路由/分片压测。

## 参考

- [XXL-Job](https://github.com/xuxueli/xxl-job)
- [XXL-Job 文档](https://www.xuxueli.com/xxl-job/)
- 本项目根 README 的统一模块约定
