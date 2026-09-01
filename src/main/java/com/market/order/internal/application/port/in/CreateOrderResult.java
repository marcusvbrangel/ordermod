package com.market.order.internal.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record CreateOrderResult(UUID orderId) {

    public CreateOrderResult {
        Objects.requireNonNull(orderId, "orderId é obrigatório");
    }
}
