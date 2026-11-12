CREATE TABLE IF NOT EXISTS kafka_processed_order (
    order_id VARCHAR(128) NOT NULL,
    processed_at DATETIME NOT NULL,
    PRIMARY KEY (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS kafka_failed_order (
    order_id VARCHAR(128) NOT NULL,
    failed_at DATETIME NOT NULL,
    PRIMARY KEY (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
