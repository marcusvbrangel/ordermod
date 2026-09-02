ALTER TABLE orders.orders
    ADD COLUMN status VARCHAR(40),
    ADD COLUMN total_amount NUMERIC(19, 2),
    ADD COLUMN currency CHAR(3);

ALTER TABLE orders.order_items
    ADD COLUMN unit_price NUMERIC(19, 2),
    ADD COLUMN subtotal NUMERIC(19, 2);

ALTER TABLE orders.orders
    ADD CONSTRAINT ck_orders_status
        CHECK (status IN (
            'AGUARDANDO_ESTOQUE',
            'AGUARDANDO_AUTORIZACAO',
            'AGUARDANDO_BAIXA_ESTOQUE',
            'AGUARDANDO_CAPTURA',
            'CONFIRMADO',
            'CANCELAMENTO_PENDENTE',
            'CANCELADO',
            'EXPIRADO'
        )),
    ADD CONSTRAINT ck_orders_total_amount_positive CHECK (total_amount > 0),
    ADD CONSTRAINT ck_orders_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT ck_orders_commercial_snapshot_complete
        CHECK (
            (status IS NULL AND total_amount IS NULL AND currency IS NULL)
            OR
            (status IS NOT NULL AND total_amount IS NOT NULL AND currency IS NOT NULL)
        );

ALTER TABLE orders.order_items
    ADD CONSTRAINT ck_order_items_unit_price_positive CHECK (unit_price > 0),
    ADD CONSTRAINT ck_order_items_subtotal_positive CHECK (subtotal > 0),
    ADD CONSTRAINT ck_order_items_subtotal_matches_quantity
        CHECK (subtotal = unit_price * quantity),
    ADD CONSTRAINT ck_order_items_commercial_snapshot_complete
        CHECK (
            (unit_price IS NULL AND subtotal IS NULL)
            OR
            (unit_price IS NOT NULL AND subtotal IS NOT NULL)
        );
