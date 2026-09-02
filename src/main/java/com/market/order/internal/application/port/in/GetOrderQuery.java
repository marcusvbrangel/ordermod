package com.market.order.internal.application.port.in;

import java.util.Objects;
import java.util.UUID;

public record GetOrderQuery(UUID orderId) {

    public GetOrderQuery {
        Objects.requireNonNull(orderId, "orderId é obrigatório");
    }
}
