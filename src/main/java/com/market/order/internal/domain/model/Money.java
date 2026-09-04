package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

@ValueObject
public record Money(BigDecimal amount, String currency) {

    private static final int SCALE = 2;
    private static final int MAX_INTEGER_DIGITS = 17;

    public Money {
        if (amount == null) {
            throw new OrderDomainException("amount é obrigatório");
        }

        if (currency == null || currency.isBlank()) {
            throw new OrderDomainException("currency é obrigatória");
        }

        var normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalizedCurrency);
        } catch (IllegalArgumentException exception) {
            throw new OrderDomainException("currency inválida: " + normalizedCurrency);
        }

        try {
            amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new OrderDomainException("amount deve possuir no máximo duas casas decimais");
        }

        if (amount.signum() < 0) {
            throw new OrderDomainException("amount não pode ser negativo");
        }

        if (amount.precision() - amount.scale() > MAX_INTEGER_DIGITS) {
            throw new OrderDomainException("amount excede a precisão monetária suportada");
        }

        currency = normalizedCurrency;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(Quantity quantity) {
        if (quantity == null) {
            throw new OrderDomainException("quantity é obrigatória para calcular o valor");
        }

        return new Money(amount.multiply(BigDecimal.valueOf(quantity.value())), currency);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    private void requireSameCurrency(Money other) {
        if (other == null) {
            throw new OrderDomainException("money é obrigatório");
        }

        if (!currency.equals(other.currency)) {
            throw new OrderDomainException("não é possível operar valores de moedas diferentes");
        }
    }
}
