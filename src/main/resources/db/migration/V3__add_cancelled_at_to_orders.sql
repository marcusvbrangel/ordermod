ALTER TABLE orders.orders
    ADD COLUMN cancelled_at TIMESTAMPTZ;

-- No constraint by default; cancelled_at is nullable and only set when order is cancelled.
