CREATE TABLE IF NOT EXISTS kafka_processed_order (
    order_id VARCHAR(128) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS kafka_failed_order (
    order_id VARCHAR(128) PRIMARY KEY,
    failed_at TIMESTAMP NOT NULL
);
