DROP TABLE IF EXISTS t_order;

CREATE TABLE t_order (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL
);
