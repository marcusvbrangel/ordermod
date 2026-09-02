package com.market.order.internal.adapter.out.persistence.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "order_items", schema = "orders")
public record OrderItemJdbcEntity(
        @Id UUID id,
        @Column("product_id") UUID productId,
        @Column("quantity") int quantity,
        @Column("unit_price") BigDecimal unitPrice,
        @Column("subtotal") BigDecimal subtotal
) {
}
