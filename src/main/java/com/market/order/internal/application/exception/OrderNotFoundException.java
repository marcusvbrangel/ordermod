package com.market.order.internal.application.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Pedido não encontrado: " + orderId);
    }
}
