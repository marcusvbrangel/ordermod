package com.market.order.internal.application.service;

import com.market.order.OrderCreatedEvent;
import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.in.CreateOrderResult;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import com.market.order.internal.application.port.out.OrderEventPublisher;
import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public CreateOrderService(
            OrderRepository orderRepository,
            OrderEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public CreateOrderResult createOrder(CreateOrderCommand command) {
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

        return new CreateOrderResult(savedOrder.id());
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

        eventPublisher.publish(orderCreatedEvent);
    }
}
