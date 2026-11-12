CREATE TABLE IF NOT EXISTS storage_tbl (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    stock INT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS storage_txn (
    request_id VARCHAR(128) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS undo_log (
    branch_id BIGINT NOT NULL,
    xid VARCHAR(128) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info BLOB NOT NULL,
    log_status INT NOT NULL,
    log_created TIMESTAMP(6) NOT NULL,
    log_modified TIMESTAMP(6) NOT NULL,
    UNIQUE (xid, branch_id)
);
