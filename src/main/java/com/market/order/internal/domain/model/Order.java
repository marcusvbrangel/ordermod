package com.market.order.internal.domain.model;

import com.market.order.internal.domain.event.OrderDomainEvent;
import com.market.order.internal.domain.event.OrderPlacedDomainEvent;
import com.market.order.internal.domain.exception.OrderDomainException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final PaymentMethod paymentMethod;
    private final Instant createdAt;
    private final Integer version;
    private final List<OrderItem> items;
    private final List<OrderDomainEvent> domainEvents = new ArrayList<>();

    private Order(
            OrderId id,
            CustomerId customerId,
            PaymentMethod paymentMethod,
            Instant createdAt,
            Integer version,
            List<OrderItem> items
    ) {
        if (id == null) {
            throw new OrderDomainException("orderId é obrigatório");
        }

        if (customerId == null) {
            throw new OrderDomainException("customerId é obrigatório");
        }

        if (paymentMethod == null) {
            throw new OrderDomainException("paymentMethod é obrigatório");
        }

        if (createdAt == null) {
            throw new OrderDomainException("createdAt é obrigatório");
        }

        if (items == null || items.isEmpty()) {
            throw new OrderDomainException("items deve conter pelo menos um item");
        }

        if (items.stream().anyMatch(item -> item == null)) {
            throw new OrderDomainException("items não pode conter item nulo");
        }

        if (version != null && version < 0) {
            throw new OrderDomainException("version não pode ser negativa");
        }

        this.id = id;
        this.customerId = customerId;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.version = version;
        this.items = List.copyOf(items);
    }

    public static Order place(
            OrderId id,
            CustomerId customerId,
            PaymentMethod paymentMethod,
            Instant createdAt,
            List<OrderItem> items
    ) {
        var order = new Order(id, customerId, paymentMethod, createdAt, null, items);

        order.record(new OrderPlacedDomainEvent(
                order.id,
                order.createdAt,
                order.customerId,
                order.paymentMethod,
                order.items.stream()
                        .map(item -> new OrderPlacedDomainEvent.Item(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        ));

        return order;
    }

    public static Order reconstitute(
            OrderId id,
            CustomerId customerId,
            PaymentMethod paymentMethod,
            Instant createdAt,
            Integer version,
            List<OrderItem> items
    ) {
        return new Order(id, customerId, paymentMethod, createdAt, version, items);
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Integer version() {
        return version;
    }

    public List<OrderItem> items() {
        return items;
    }

    public List<OrderDomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    private void record(OrderDomainEvent event) {
        domainEvents.add(event);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Order order)) {
            return false;
        }

        return id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
