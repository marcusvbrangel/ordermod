package com.market.order.internal.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record GetOrderResult(
        UUID orderId,
        UUID customerId,
        String paymentMethod,
        String status,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        List<Item> items
) {

    public GetOrderResult {
        Objects.requireNonNull(orderId, "orderId é obrigatório");
        Objects.requireNonNull(customerId, "customerId é obrigatório");
        Objects.requireNonNull(paymentMethod, "paymentMethod é obrigatório");
        Objects.requireNonNull(status, "status é obrigatório");
        Objects.requireNonNull(totalAmount, "totalAmount é obrigatório");
        Objects.requireNonNull(currency, "currency é obrigatória");
        Objects.requireNonNull(createdAt, "createdAt é obrigatório");
        Objects.requireNonNull(items, "items é obrigatório");
        items = List.copyOf(items);
    }

    public record Item(
            UUID itemId,
            UUID productId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {

        public Item {
            Objects.requireNonNull(itemId, "itemId é obrigatório");
            Objects.requireNonNull(productId, "productId é obrigatório");
            Objects.requireNonNull(unitPrice, "unitPrice é obrigatório");
            Objects.requireNonNull(subtotal, "subtotal é obrigatório");
        }
    }
}
