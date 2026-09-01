package com.market.order.internal.application.port.out;

import com.market.order.OrderCreatedEvent;

public interface OrderEventPublisher {

    void publish(OrderCreatedEvent event);
}
