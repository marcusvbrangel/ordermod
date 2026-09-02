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
    private final OrderStatus status;
    private final Money total;
    private final Instant createdAt;
    private final Instant cancelledAt;
    private final Integer version;
    private final List<OrderItem> items;
    private final List<OrderDomainEvent> domainEvents = new ArrayList<>();

    private Order(
            OrderId id,
            CustomerId customerId,
            PaymentMethod paymentMethod,
            OrderStatus status,
            Money total,
            Instant createdAt,
            Instant cancelledAt,
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

        if (status == null) {
            throw new OrderDomainException("status é obrigatório");
        }

        if (total == null || !total.isPositive()) {
            throw new OrderDomainException("total deve ser maior que zero");
        }

        if (createdAt == null) {
            throw new OrderDomainException("createdAt é obrigatório");
        }

        if (cancelledAt != null && status != OrderStatus.CANCELADO) {
            throw new OrderDomainException("cancelledAt só pode existir quando status = CANCELADO");
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

        var calculatedTotal = calculateTotal(items);
        if (!total.equals(calculatedTotal)) {
            throw new OrderDomainException("total deve corresponder à soma dos subtotais");
        }

        this.id = id;
        this.customerId = customerId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.total = total;
        this.createdAt = createdAt;
        this.cancelledAt = cancelledAt;
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
        var total = calculateTotal(items);
        var order = new Order(
                id,
                customerId,
                paymentMethod,
                OrderStatus.AGUARDANDO_ESTOQUE,
                total,
                createdAt,
                /* cancelledAt */ null,
                /* version */ null,
                items
        );

        order.record(new OrderPlacedDomainEvent(
                order.id,
                order.createdAt,
                order.customerId,
                order.paymentMethod,
                order.status,
                order.total,
                order.items.stream()
                        .map(item -> new OrderPlacedDomainEvent.Item(
                                item.productId(),
                                item.quantity(),
                                item.unitPrice(),
                                item.subtotal()
                        ))
                        .toList()
        ));

        return order;
    }

    public static Order reconstitute(
            OrderId id,
            CustomerId customerId,
            PaymentMethod paymentMethod,
            OrderStatus status,
            Money total,
            Instant createdAt,
            Instant cancelledAt,
            Integer version,
            List<OrderItem> items
    ) {
        return new Order(id, customerId, paymentMethod, status, total, createdAt, cancelledAt, version, items);
    }

    /**
     * Return a new Order instance representing this order cancelled.
     * This method does not register domain events; cancellation is a state change persisted by the application.
     */
    public Order cancel() {
        if (this.status == OrderStatus.CANCELADO) {
            return this;
        }

        var cancelledAt = Instant.now();
        var cancelled = Order.reconstitute(
                this.id,
                this.customerId,
                this.paymentMethod,
                OrderStatus.CANCELADO,
                this.total,
                this.createdAt,
                cancelledAt,
                this.version,
                this.items
        );

        // record a domain event for cancellation
        cancelled.record(new com.market.order.internal.domain.event.OrderCanceledDomainEvent(
                cancelled.id,
                cancelledAt,
                cancelled.customerId,
                cancelledAt
        ));

        return cancelled;
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

    public OrderStatus status() {
        return status;
    }

    public Money total() {
        return total;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant cancelledAt() {
        return cancelledAt;
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

    private static Money calculateTotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new OrderDomainException("items deve conter pelo menos um item");
        }

        if (items.stream().anyMatch(Objects::isNull)) {
            throw new OrderDomainException("items não pode conter item nulo");
        }

        var total = items.getFirst().subtotal();
        for (var item : items.subList(1, items.size())) {
            total = total.add(item.subtotal());
        }
        return total;
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
