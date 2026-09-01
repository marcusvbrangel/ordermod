package com.market.order.internal.application.port.out;

import com.market.order.internal.domain.model.Order;

public interface OrderRepository {

    Order save(Order order);
}
