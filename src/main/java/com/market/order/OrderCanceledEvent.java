package com.market.order;

import java.time.Instant;
import java.util.UUID;

public record OrderCanceledEvent(
        UUID orderId,
        Instant cancelledAt,
        UUID customerId
) {
}
