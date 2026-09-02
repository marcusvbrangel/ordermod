package com.market.order.internal.application.port.in;

import java.time.Instant;
import java.util.UUID;

public record CancelOrderResult(UUID orderId, String status, Instant cancelledAt) {

}
