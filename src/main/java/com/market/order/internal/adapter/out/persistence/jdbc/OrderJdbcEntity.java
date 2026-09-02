package com.market.order.internal.adapter.out.persistence.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table(name = "orders", schema = "orders")
public record OrderJdbcEntity(
        @Id UUID id,
        @Column("customer_id") UUID customerId,
        @Column("payment_method") String paymentMethod,
        @Column("status") String status,
        @Column("total_amount") BigDecimal totalAmount,
        @Column("currency") String currency,
        @Column("created_at") Instant createdAt,
        @Version @Column("version") Integer version,
        @MappedCollection(idColumn = "order_id", keyColumn = "item_index")
        List<OrderItemJdbcEntity> items
) {

    public OrderJdbcEntity {
        items = List.copyOf(items);
    }
}
