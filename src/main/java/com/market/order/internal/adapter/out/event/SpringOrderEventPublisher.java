package com.market.order.internal.adapter.out.event;

import com.market.order.OrderCreatedEvent;
import com.market.order.internal.application.port.out.OrderEventPublisher;
import com.market.order.internal.domain.event.OrderDomainEvent;
import com.market.order.internal.domain.event.OrderPlacedDomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringOrderEventPublisher implements OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public SpringOrderEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(OrderDomainEvent event) {
        if (!(event instanceof OrderPlacedDomainEvent placedEvent)) {
            throw new IllegalArgumentException("Evento de domínio não suportado: " + event.getClass().getName());
        }

        eventPublisher.publishEvent(new OrderCreatedEvent(
                placedEvent.orderId().value(),
                placedEvent.occurredAt(),
                placedEvent.customerId().value(),
                placedEvent.paymentMethod().value(),
                placedEvent.items().stream()
                        .map(item -> new OrderCreatedEvent.Item(
                                item.productId().value(),
                                item.quantity().value()
                        ))
                        .toList()
        ));
    }
}
