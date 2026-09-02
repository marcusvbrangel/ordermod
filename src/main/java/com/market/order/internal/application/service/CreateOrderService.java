package com.market.order.internal.application.service;

import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.in.CreateOrderResult;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import com.market.order.internal.application.port.out.OrderEventPublisher;
import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Money;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderItem;
import com.market.order.internal.domain.model.OrderItemId;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;
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

        var order = Order.place(
                new OrderId(UUID.randomUUID()),
                new CustomerId(command.customerId()),
                new PaymentMethod(command.paymentMethod()),
                Instant.now(),
                command.items().stream()
                        .map(item -> OrderItem.create(
                                new OrderItemId(UUID.randomUUID()),
                                new ProductId(item.productId()),
                                new Quantity(item.quantity()),
                                new Money(item.unitPrice(), command.currency())
                        ))
                        .toList()
        );

        var savedOrder = orderRepository.save(order);

        publishDomainEvents(order);

        return new CreateOrderResult(
                savedOrder.id().value(),
                savedOrder.status().name(),
                savedOrder.total().amount(),
                savedOrder.total().currency(),
                savedOrder.items().stream()
                        .map(item -> new CreateOrderResult.Item(
                                item.productId().value(),
                                item.quantity().value(),
                                item.unitPrice().amount(),
                                item.subtotal().amount()
                        ))
                        .toList()
        );
    }

    private void publishDomainEvents(Order order) {
        order.domainEvents().forEach(eventPublisher::publish);
        order.clearDomainEvents();
    }
}
