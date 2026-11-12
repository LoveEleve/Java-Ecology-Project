CREATE TABLE IF NOT EXISTS rocketmq_processed_order (
    order_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    processed_at DATETIME NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS rocketmq_transaction_order (
    order_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
