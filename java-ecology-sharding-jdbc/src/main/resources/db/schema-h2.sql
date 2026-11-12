DROP TABLE IF EXISTS t_order_0;
DROP TABLE IF EXISTS t_order_1;

CREATE TABLE t_order_0 (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE t_order_1 (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL
);

DROP TABLE IF EXISTS t_order_item_0;
DROP TABLE IF EXISTS t_order_item_1;

CREATE TABLE t_order_item_0 (
    item_id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    product_name VARCHAR(64) NOT NULL,
    quantity INT NOT NULL
);

CREATE TABLE t_order_item_1 (
    item_id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    product_name VARCHAR(64) NOT NULL,
    quantity INT NOT NULL
);

DROP TABLE IF EXISTS t_order_hint_0;
DROP TABLE IF EXISTS t_order_hint_1;

CREATE TABLE t_order_hint_0 (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE t_order_hint_1 (
    order_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL
);
