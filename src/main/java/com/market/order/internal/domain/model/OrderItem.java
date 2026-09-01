package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;

import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID productId,
        int quantity
) {

    public OrderItem {
        if (id == null) {
            throw new OrderDomainException("id é obrigatório");
        }

        if (productId == null) {
            throw new OrderDomainException("productId é obrigatório");
        }

        if (quantity <= 0) {
            throw new OrderDomainException("quantity deve ser maior que zero");
        }
    }
}
