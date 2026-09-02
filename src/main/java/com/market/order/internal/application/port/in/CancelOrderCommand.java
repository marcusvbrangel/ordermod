package com.market.order.internal.application.port.in;

import java.util.UUID;

public record CancelOrderCommand(UUID orderId) {

    public CancelOrderCommand {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId é obrigatório");
        }
    }
}
