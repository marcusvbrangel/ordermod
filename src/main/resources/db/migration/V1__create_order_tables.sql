CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE orders.orders
(
    id             UUID         NOT NULL,
    customer_id    UUID         NOT NULL,
    payment_method VARCHAR(50)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    version        INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT ck_orders_payment_method_not_blank
        CHECK (BTRIM(payment_method) <> '')
);

CREATE TABLE orders.order_items
(
    id         UUID     NOT NULL,
    order_id   UUID     NOT NULL,
    product_id UUID     NOT NULL,
    quantity   INTEGER  NOT NULL,
    item_index INTEGER  NOT NULL,

    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
            REFERENCES orders.orders (id)
            ON DELETE CASCADE,
    CONSTRAINT uq_order_items_order_index UNIQUE (order_id, item_index),
    CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_order_items_index_non_negative CHECK (item_index >= 0)
);

CREATE INDEX idx_order_items_order_id
    ON orders.order_items (order_id);
