package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;

import java.util.Objects;

public final class OrderItem {

    private final OrderItemId id;
    private final ProductId productId;
    private final Quantity quantity;

    private OrderItem(OrderItemId id, ProductId productId, Quantity quantity) {
        if (id == null) {
            throw new OrderDomainException("orderItemId é obrigatório");
        }

        if (productId == null) {
            throw new OrderDomainException("productId é obrigatório");
        }

        if (quantity == null) {
            throw new OrderDomainException("quantity é obrigatória");
        }

        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
    }

    public static OrderItem create(OrderItemId id, ProductId productId, Quantity quantity) {
        return new OrderItem(id, productId, quantity);
    }

    public static OrderItem reconstitute(OrderItemId id, ProductId productId, Quantity quantity) {
        return new OrderItem(id, productId, quantity);
    }

    public OrderItemId id() {
        return id;
    }

    public ProductId productId() {
        return productId;
    }

    public Quantity quantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof OrderItem orderItem)) {
            return false;
        }

        return id.equals(orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
