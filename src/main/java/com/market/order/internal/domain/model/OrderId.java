package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

@ValueObject
public record OrderId(UUID value) {

    public OrderId {
        if (value == null) {
            throw new OrderDomainException("orderId é obrigatório");
        }
    }
}
