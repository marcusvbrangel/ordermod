package com.market.order.internal.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Table(name = "orders", schema = "orders")
public record Order(
        @Id UUID id,
        UUID customerId,
        String paymentMethod,
        Instant createdAt,
        @Version Integer version,
        @MappedCollection(idColumn = "order_id", keyColumn = "item_index")
        List<OrderItem> items
) {

    public Order {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(customerId, "customerId é obrigatório");
        Objects.requireNonNull(paymentMethod, "paymentMethod é obrigatório");
        Objects.requireNonNull(createdAt, "createdAt é obrigatório");
        Objects.requireNonNull(items, "items é obrigatório");

        paymentMethod = paymentMethod.strip();

        if (paymentMethod.isEmpty()) {
            throw new IllegalArgumentException("paymentMethod não pode estar vazio");
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("items deve conter pelo menos um item");
        }

        items = List.copyOf(items);
    }
}
