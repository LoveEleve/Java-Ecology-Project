-- 预算账户：一个活动 + 资金来源 + 券模板 + 单位 = 一个账户。
CREATE TABLE IF NOT EXISTS t_budget_account (
    activity_id      VARCHAR(64)   NOT NULL,
    source_id        VARCHAR(64)   NOT NULL,
    template_id      VARCHAR(64)   NOT NULL,
    unit             VARCHAR(16)   NOT NULL,
    total_amount     DECIMAL(18,4) NOT NULL,
    reserved_amount  DECIMAL(18,4) NOT NULL DEFAULT 0,
    consumed_amount  DECIMAL(18,4) NOT NULL DEFAULT 0,
    budget_version   BIGINT        NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP(3)  NOT NULL,
    PRIMARY KEY (activity_id, source_id, template_id, unit),
    CONSTRAINT ck_budget_account_available CHECK (total_amount >= reserved_amount + consumed_amount)
);

-- 预算流水：request_id 唯一做预占幂等；txn_id 做 confirm/release 幂等。
CREATE TABLE IF NOT EXISTS t_budget_flow (
    txn_id         VARCHAR(64)   NOT NULL,
    request_id     VARCHAR(64)   NOT NULL,
    request_hash   VARCHAR(128)  NOT NULL,
    activity_id    VARCHAR(64)   NOT NULL,
    source_id      VARCHAR(64)   NOT NULL,
    template_id    VARCHAR(64)   NOT NULL,
    unit           VARCHAR(16)   NOT NULL,
    amount         DECIMAL(18,4) NOT NULL,
    lease_id       VARCHAR(64),
    status         VARCHAR(16)   NOT NULL,
    created_at     TIMESTAMP(3)  NOT NULL,
    updated_at     TIMESTAMP(3)  NOT NULL,
    PRIMARY KEY (txn_id),
    UNIQUE (request_id)
);

CREATE INDEX IF NOT EXISTS idx_budget_flow_expiry
    ON t_budget_flow (status, created_at);

-- 预算号段：leaseRequestId 唯一做“申请响应丢失”的幂等。
CREATE TABLE IF NOT EXISTS t_budget_lease (
    lease_id          VARCHAR(64)   NOT NULL,
    lease_request_id  VARCHAR(64)   NOT NULL,
    instance_id       VARCHAR(64)   NOT NULL,
    activity_id       VARCHAR(64)   NOT NULL,
    source_id         VARCHAR(64)   NOT NULL,
    template_id       VARCHAR(64)   NOT NULL,
    unit              VARCHAR(16)   NOT NULL,
    chunk_amount      DECIMAL(18,4) NOT NULL,
    used_amount       DECIMAL(18,4) NOT NULL DEFAULT 0,
    status            VARCHAR(16)   NOT NULL,
    expire_at         TIMESTAMP(3)  NOT NULL,
    created_at        TIMESTAMP(3)  NOT NULL,
    updated_at        TIMESTAMP(3)  NOT NULL,
    PRIMARY KEY (lease_id),
    UNIQUE (lease_request_id)
);

CREATE INDEX IF NOT EXISTS idx_budget_lease_active
    ON t_budget_lease (status, expire_at);
