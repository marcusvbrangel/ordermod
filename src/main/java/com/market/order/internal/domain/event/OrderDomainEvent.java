package com.market.order.internal.domain.event;

import java.time.Instant;

public sealed interface OrderDomainEvent permits OrderPlacedDomainEvent, OrderCanceledDomainEvent {

    Instant occurredAt();
}
