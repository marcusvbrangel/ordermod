package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record PaymentMethod(String value) {

    public PaymentMethod {
        if (value == null) {
            throw new OrderDomainException("paymentMethod é obrigatório");
        }

        value = value.strip();

        if (value.isEmpty()) {
            throw new OrderDomainException("paymentMethod não pode estar vazio");
        }
    }
}
