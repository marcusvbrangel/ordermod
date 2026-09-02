package com.market.order.internal.application.exception;

import java.util.UUID;

public class OrderNotCancellableException extends RuntimeException {

    public OrderNotCancellableException(UUID orderId, String status) {
        super("Pedido não pode ser cancelado no estado: " + status + " (orderId=" + orderId + ")");
    }
}
