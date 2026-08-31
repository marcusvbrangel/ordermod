package com.market.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        Instant createdAt,
        UUID customerId,
        String paymentMethod,
        List<Item> items
) {

    public OrderCreatedEvent {
        items = List.copyOf(items);
    }

    public record Item(
            UUID productId,
            int quantity
    ) {
    }
}
