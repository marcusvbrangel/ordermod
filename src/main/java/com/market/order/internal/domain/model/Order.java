package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        UUID customerId,
        String paymentMethod,
        Instant createdAt,
        Integer version,
        List<OrderItem> items
) {

    public Order {
        if (id == null) {
            throw new OrderDomainException("id é obrigatório");
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

        if (items == null) {
            throw new OrderDomainException("items é obrigatório");
        }

        paymentMethod = paymentMethod.strip();

        if (paymentMethod.isEmpty()) {
            throw new OrderDomainException("paymentMethod não pode estar vazio");
        }

        if (items.isEmpty()) {
            throw new OrderDomainException("items deve conter pelo menos um item");
        }

        if (items.stream().anyMatch(item -> item == null)) {
            throw new OrderDomainException("items não pode conter item nulo");
        }

        if (version != null && version < 0) {
            throw new OrderDomainException("version não pode ser negativa");
        }

        items = List.copyOf(items);
    }
}
