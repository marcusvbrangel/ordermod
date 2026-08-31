package com.market.order.internal.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;
import java.util.UUID;

@Table(name = "order_items", schema = "orders")
public record OrderItem(
        @Id UUID id,
        UUID productId,
        int quantity
) {

    public OrderItem {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(productId, "productId é obrigatório");

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity deve ser maior que zero");
        }
    }
}
