package com.market.order.internal.application.port.in;

public interface CreateOrderUseCase {

    CreateOrderResult createOrder(CreateOrderCommand command);
}
