package com.market.order.internal.application.port.in;

public interface CancelOrderUseCase {

    CancelOrderResult cancelOrder(CancelOrderCommand command);
}
