package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.util.Objects;

@Entity
public final class OrderItem {

    @Identity
    private final OrderItemId id;
    private final ProductId productId;
    private final Quantity quantity;
    private final Money unitPrice;
    private final Money subtotal;

    private OrderItem(
            OrderItemId id,
            ProductId productId,
            Quantity quantity,
            Money unitPrice,
            Money subtotal
    ) {
        if (id == null) {
            throw new OrderDomainException("orderItemId é obrigatório");
        }

        if (productId == null) {
            throw new OrderDomainException("productId é obrigatório");
        }

        if (quantity == null) {
            throw new OrderDomainException("quantity é obrigatória");
        }

        if (unitPrice == null || !unitPrice.isPositive()) {
            throw new OrderDomainException("unitPrice deve ser maior que zero");
        }

        if (subtotal == null) {
            throw new OrderDomainException("subtotal é obrigatório");
        }

        if (!unitPrice.multiply(quantity).equals(subtotal)) {
            throw new OrderDomainException("subtotal deve corresponder a unitPrice multiplicado por quantity");
        }

        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public static OrderItem create(OrderItemId id, ProductId productId, Quantity quantity, Money unitPrice) {
        if (unitPrice == null) {
            throw new OrderDomainException("unitPrice é obrigatório");
        }

        return new OrderItem(id, productId, quantity, unitPrice, unitPrice.multiply(quantity));
    }

    public static OrderItem reconstitute(
            OrderItemId id,
            ProductId productId,
            Quantity quantity,
            Money unitPrice,
            Money subtotal
    ) {
        return new OrderItem(id, productId, quantity, unitPrice, subtotal);
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

    public Money unitPrice() {
        return unitPrice;
    }

    public Money subtotal() {
        return subtotal;
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
