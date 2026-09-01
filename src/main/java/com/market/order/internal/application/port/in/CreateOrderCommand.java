package com.market.order.internal.application.port.in;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
        UUID customerId,
        String paymentMethod,
        List<Item> items
) {

    public record Item(
            UUID productId,
            int quantity
    ) {
    }
}
