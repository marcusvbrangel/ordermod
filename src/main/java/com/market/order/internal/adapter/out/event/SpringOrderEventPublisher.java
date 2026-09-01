package com.market.order.internal.adapter.out.event;

import com.market.order.OrderCreatedEvent;
import com.market.order.internal.application.port.out.OrderEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringOrderEventPublisher implements OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public SpringOrderEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
