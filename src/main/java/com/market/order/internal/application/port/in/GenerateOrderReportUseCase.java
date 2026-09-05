package com.market.order.internal.application.port.in;

public interface GenerateOrderReportUseCase {
    GenerateOrderReportResult execute(GenerateOrderReportQuery query);
}
