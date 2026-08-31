package com.market.order.internal.application;

import com.market.order.OrderCreatedEvent;
import com.market.order.internal.domain.Order;
import com.market.order.internal.domain.OrderItem;
import com.market.order.internal.infrastructure.persistence.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void createOrder(CreateOrderCommand command) {

        Objects.requireNonNull(command, "command é obrigatório");

        var order = new Order(
                UUID.randomUUID(),
                command.customerId(),
                command.paymentMethod(),
                Instant.now(),
                null,
                command.items().stream()
                        .map(item -> new OrderItem(
                                UUID.randomUUID(),
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );

        var savedOrder = orderRepository.save(order);

        publishOrderCreatedEvent(savedOrder);
    }

    private void publishOrderCreatedEvent(Order order) {

        var orderCreatedEvent = new OrderCreatedEvent(
                order.id(),
                order.createdAt(),
                order.customerId(),
                order.paymentMethod(),
                order.items().stream()
                        .map(item -> new OrderCreatedEvent.Item(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );

        eventPublisher.publishEvent(orderCreatedEvent);
    }
}
