CREATE TABLE IF NOT EXISTS rocketmq_processed_order (
    order_id VARCHAR(128) PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    processed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS rocketmq_transaction_order (
    order_id VARCHAR(128) PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
