package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

@ValueObject
public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new OrderDomainException("customerId é obrigatório");
        }
    }
}
