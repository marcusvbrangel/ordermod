package com.market.order.internal.application.port.out;

import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;

import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId orderId);
}
