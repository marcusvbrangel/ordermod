package com.market.order.internal.application.port.in;

import java.util.List;

public record GenerateOrderReportResult(List<OrderReportDTO> records) {
}
