package com.market.order.internal.application.port.out;

import com.market.order.internal.domain.event.OrderDomainEvent;

public interface OrderEventPublisher {

    void publish(OrderDomainEvent event);
}
