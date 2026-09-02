package com.market.order.internal.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

public record CancelOrderResponse(UUID orderId, String status, Instant cancelledAt) {

}
