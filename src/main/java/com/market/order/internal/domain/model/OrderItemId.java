package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

@ValueObject
public record OrderItemId(UUID value) {

    public OrderItemId {
        if (value == null) {
            throw new OrderDomainException("orderItemId é obrigatório");
        }
    }
}
