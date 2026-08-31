package com.market.order.internal.application;

import com.market.order.OrderCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class OrderService {

    private final ApplicationEventPublisher event;

    public OrderService(ApplicationEventPublisher event) {
        this.event = event;
    }

    @Transactional
    public void createOrder(CreateOrderCommand command) {
        Objects.requireNonNull(command, "command é obrigatório");

        var orderCreatedEvent = new OrderCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                command.customerId(),
                command.paymentMethod(),
                command.items().stream()
                        .map(item -> new OrderCreatedEvent.Item(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );

        event.publishEvent(orderCreatedEvent);
    }
}
