package com.market.order.internal.application.port.in;

import java.time.LocalDate;

public record GenerateOrderReportQuery(LocalDate startDate, LocalDate endDate, String status) {
}
