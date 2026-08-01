-- Demo sales table for local development (H2)
CREATE TABLE IF NOT EXISTS sales (
    id          BIGINT PRIMARY KEY,
    order_date  TIMESTAMP,
    region      VARCHAR(50),
    product     VARCHAR(100),
    category    VARCHAR(50),
    amount      DECIMAL(12,2),
    quantity    INT,
    status      VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS users (
    id             BIGINT PRIMARY KEY,
    register_date  TIMESTAMP,
    city           VARCHAR(50),
    age            INT,
    is_active      BOOLEAN,
    last_login     TIMESTAMP
);
