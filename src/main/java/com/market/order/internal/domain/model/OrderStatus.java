package com.market.order.internal.domain.model;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public enum OrderStatus {
    AGUARDANDO_ESTOQUE,
    AGUARDANDO_AUTORIZACAO,
    AGUARDANDO_BAIXA_ESTOQUE,
    AGUARDANDO_CAPTURA,
    CONFIRMADO,
    CANCELAMENTO_PENDENTE,
    CANCELADO,
    EXPIRADO
}
