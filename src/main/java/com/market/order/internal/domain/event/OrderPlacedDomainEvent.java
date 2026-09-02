package com.market.order.internal.domain.event;

import com.market.order.internal.domain.exception.OrderDomainException;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Money;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderStatus;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;

import java.time.Instant;
import java.util.List;

public record OrderPlacedDomainEvent(
        OrderId orderId,
        Instant occurredAt,
        CustomerId customerId,
        PaymentMethod paymentMethod,
        OrderStatus status,
        Money total,
        List<Item> items
) implements OrderDomainEvent {

    public OrderPlacedDomainEvent {
        if (orderId == null) {
            throw new OrderDomainException("orderId é obrigatório no evento de domínio");
        }

        if (occurredAt == null) {
            throw new OrderDomainException("occurredAt é obrigatório no evento de domínio");
        }

        if (customerId == null) {
            throw new OrderDomainException("customerId é obrigatório no evento de domínio");
        }

        if (paymentMethod == null) {
            throw new OrderDomainException("paymentMethod é obrigatório no evento de domínio");
        }

        if (status == null) {
            throw new OrderDomainException("status é obrigatório no evento de domínio");
        }

        if (total == null || !total.isPositive()) {
            throw new OrderDomainException("total deve ser maior que zero no evento de domínio");
        }

        if (items == null || items.isEmpty()) {
            throw new OrderDomainException("items é obrigatório no evento de domínio");
        }

        if (items.stream().anyMatch(item -> item == null)) {
            throw new OrderDomainException("items não pode conter item nulo no evento de domínio");
        }

        items = List.copyOf(items);

        var calculatedTotal = items.stream()
                .map(Item::subtotal)
                .reduce(Money::add)
                .orElseThrow();
        if (!total.equals(calculatedTotal)) {
            throw new OrderDomainException("total inválido no evento de domínio");
        }
    }

    public record Item(ProductId productId, Quantity quantity, Money unitPrice, Money subtotal) {

        public Item {
            if (productId == null) {
                throw new OrderDomainException("productId é obrigatório no evento de domínio");
            }

            if (quantity == null) {
                throw new OrderDomainException("quantity é obrigatória no evento de domínio");
            }

            if (unitPrice == null || !unitPrice.isPositive()) {
                throw new OrderDomainException("unitPrice deve ser maior que zero no evento de domínio");
            }

            if (subtotal == null || !unitPrice.multiply(quantity).equals(subtotal)) {
                throw new OrderDomainException("subtotal inválido no evento de domínio");
            }
        }
    }
}
