package com.market.order.internal.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderReportDTO(
        UUID orderId,
        UUID customerId,
        String status,
        BigDecimal totalValue,
        LocalDateTime createdAt,
        long totalItems
) {

}
