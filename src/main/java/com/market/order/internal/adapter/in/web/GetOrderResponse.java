package com.market.order.internal.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetOrderResponse(
        UUID orderId,
        UUID customerId,
        String paymentMethod,
        String status,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        List<Item> items
) {

    public GetOrderResponse {
        items = List.copyOf(items);
    }

    public record Item(
            UUID itemId,
            UUID productId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
    }
}
