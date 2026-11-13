# java-ecology-budget

Spring Boot 生产口径的营销预算预扣服务示例。只关注预算预扣，不实现权益发放、券中心、核销、归因。

## 定位

> 预算预扣是发券前的资金闸门：调用方先 `reserve` 占额度，成功 `confirm` 确认消耗，失败 `release` 释放。

覆盖：

- 活动总预算、来源池总额、券模板子预算；
- 多资金来源优先级/fallback；
- `reserve / confirm / release` 幂等；
- 预占超时回收；
- 本地号段申请、号段内预占、号段归还；
- EWMA + AIMD 动态号段大小（算法参考 `lease` 包）；
- 账户、流水、租约三方对账。

不覆盖：

- 券模板、券码、核销；
- 支付；
- 标签、ID-Mapping、归因模型；
- 真实 Redis/MySQL 集群接入（H2/MySQL profile 表达落库方式）。

## 技术栈

- JDK 17
- Spring Boot 3.2.5
- Spring Web / Validation / Actuator / JDBC
- H2（默认本地演示，MySQL 模式）
- MySQL（`mysql` profile）
- Maven Wrapper

## 目录结构

```text
src/main/java/com/loveeleve/ecology/budget/
  BudgetApplication.java            Spring Boot 启动类
  api/                             HTTP 接口、DTO、异常处理
  config/                          BudgetProperties、事务、超时任务
  domain/                          预算账户、流水、租约、请求/结果
  exception/                       统一业务异常与错误码
  service/BudgetJdbcService.java   生产主链路：JDBC + 本地事务
  store/InMemoryBudgetStore.java   纯内存参考实现（不是生产主链路）
  lease/                           本地号段状态机、EWMA、自适应算法
  demo/BudgetDemo.java             不依赖 Spring 的端到端演示
src/main/resources/
  application.yml                  H2 默认配置
  application-mysql.yml            MySQL 生产配置
  db/schema-h2.sql                 账户/流水/租约表
  db/data-h2.sql                   演示账户
src/test/java/.../BudgetJdbcServiceTests.java
```

## 生产主链路

```text
上游 requestId
  -> 幂等检查（t_budget_flow.request_id 唯一）
  -> 选择资金来源（优先级 / fallback）
  -> 本地事务：扣来源池总额 + 券模板子预算
  -> 发券成功后 confirm
  -> 失败/超时 release
```

核心类：`BudgetJdbcService`。

### 1. 预占

`POST /api/budget/reservations`

```json
{
  "requestId": "R1",
  "uin": "U1",
  "activityId": "A1",
  "templateId": "T1",
  "unit": "AMOUNT",
  "amount": 20,
  "sources": ["market", "product"]
}
```

规则：

- `requestId` 唯一，重复请求返回同一个 `txnId`；
- 同一 `requestId` 参数不一致返回 `IDEMPOTENCY_CONFLICT`；
- 首选来源不足自动尝试下一个来源；
- 总额/子预算任一不足整体回滚，不留半扣。

### 2. 确认 / 释放

```text
POST /api/budget/reservations/{txnId}/confirm
POST /api/budget/reservations/{txnId}/release
```

- 重复 confirm 幂等返回 CONFIRMED；
- 重复 release 幂等返回 RELEASED；
- 已释放再 confirm 或已确认再 release 返回 409。

### 3. 追加预算

`POST /api/budget/accounts/append`，只增不减。

### 4. 号段

```text
POST /api/budget/leases
POST /api/budget/leases/{leaseId}/reservations
POST /api/budget/leases/{leaseId}/return
```

- `leaseRequestId` 唯一键做申请幂等；
- 号段内预占只消耗 `t_budget_lease.used_amount`；
- 归还未用额度，已用部分保持；
- 本地自适应算法参考 `lease/LeaseManager`、`AdaptiveChunkSizer`、`EwmaRate`。

### 5. 查询

```text
GET /api/budget/accounts?activityId=A1
GET /api/budget/flows/{txnId}
```

## 运行

```bash
export JAVA_HOME=/env/javaEnv/jdk17
export PATH="$JAVA_HOME/bin:$PATH"

# 构建 + 测试
/data/workspace/source-code/Java-Ecology-Project/mvnw \
  -pl java-ecology-budget -am test

# 启动（H2 默认）
/data/workspace/source-code/Java-Ecology-Project/mvnw \
  -pl java-ecology-budget spring-boot:run

# 或运行 jar
java -jar java-ecology-budget/target/java-ecology-budget.jar
```

默认端口 8084。MySQL 生产 profile：

```bash
export DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=budget
export DB_USER=budget DB_PASSWORD=change-me
java -jar java-ecology-budget/target/java-ecology-budget.jar \
  --spring.profiles.active=mysql
```

## 表结构

- `t_budget_account`：活动 + 来源 + 模板 + 单位，total/reserved/consumed/version；
- `t_budget_flow`：requestId 唯一，status 控制 confirm/release；
- `t_budget_lease`：leaseRequestId 唯一，chunk/used/status/expire_at。

## 关键设计

### 1. 金额/数量不混用

`BudgetUnit` 区分 `AMOUNT` 和 `QUANTITY`。一个账户只有一种单位。

### 2. 多来源

账户键：

```text
activityId + sourceId + templateId + unit
```

来源池总额使用 `__TOTAL__` 作为 `templateId`；具体券模板使用真实 `templateId`。

### 3. 原子预占

`reserve` 给每个来源一个独立事务：

- 来源 A 不足 -> 该事务回滚；
- 继续尝试来源 B；
- 来源池、子预算、流水在同一事务内，任一失败整体回滚。

### 4. 幂等

- `requestId`：预占幂等；
- `leaseRequestId`：号段申请幂等；
- `txnId`：confirm/release 幂等；
- `requestHash`：同 requestId 参数不一致返回冲突。

### 5. 超时回收

`BudgetExpiryJob` 定时扫 `status=RESERVED AND created_at < now - maxHold`，逐条 release。

生产建议换 XXL-Job + `SELECT ... FOR UPDATE SKIP LOCKED`，避免每个实例重复扫同一批。

### 6. 对账

```text
account.reserved = sum(active lease 未用) + sum(flow RESERVED)
account.consumed = sum(flow CONFIRMED)
lease.used       = sum(该 lease 下 RESERVED + CONFIRMED flow)
```

账户是汇总，流水是明细，租约是额度凭证；差异优先以流水/下游业务记录修复。

## 测试

`BudgetJdbcServiceTests`：

- requestId 幂等；
- 重复 confirm 幂等；
- 号段预占/确认/归还；
- 同 requestId 参数冲突。

当前：3 tests, 0 failures。

## 生产注意事项

- H2 只用于本地演示/测试；生产用 MySQL profile；
- 真实环境建议把超时回收放 XXL-Job，不要每个实例各扫；
- 号段申请必须用 `leaseRequestId` 幂等，响应丢失用同一个 ID 回查；
- 本地号段和中央账户要对账；
- 活动结束/预算关闭要把本地状态置 `TERMINAL`；
- 预算追加通过 `budget_version`/事件/按需探测恢复实例；
- 不要用最新 `budget_version` 校验 confirm/release，避免误伤在途流水。
