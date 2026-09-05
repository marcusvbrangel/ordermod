package com.market.order.internal.application.service;

import com.market.order.internal.application.port.in.GenerateOrderReportQuery;
import com.market.order.internal.application.port.in.GenerateOrderReportResult;
import com.market.order.internal.application.port.in.GenerateOrderReportUseCase;
import com.market.order.internal.application.port.out.OrderQueryRepository;
import org.springframework.stereotype.Service;

@Service
public class GenerateOrderReportService implements GenerateOrderReportUseCase {

    private final OrderQueryRepository orderQueryRepository;

    public GenerateOrderReportService(OrderQueryRepository orderQueryRepository) {
        this.orderQueryRepository = orderQueryRepository;
    }

    @Override
    public GenerateOrderReportResult execute(GenerateOrderReportQuery query) {

        var records = orderQueryRepository.findReportByCriteria(query);
        return new GenerateOrderReportResult(records);

    }

}
