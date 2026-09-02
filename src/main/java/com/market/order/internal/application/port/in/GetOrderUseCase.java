package com.market.order.internal.application.port.in;

public interface GetOrderUseCase {

    GetOrderResult getOrder(GetOrderQuery query);
}
