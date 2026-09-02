package com.market.order.internal.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderResponse(
        UUID orderId,
        String status,
        BigDecimal totalAmount,
        String currency,
        List<Item> items
) {

    public CreateOrderResponse {
        items = List.copyOf(items);
    }

    public record Item(
            UUID productId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
    }
}
