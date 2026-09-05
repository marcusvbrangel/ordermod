package com.market.order.internal.application.port.out;

import com.market.order.internal.application.port.in.GenerateOrderReportQuery;
import com.market.order.internal.application.port.in.OrderReportDTO;

import java.util.List;

public interface OrderQueryRepository {

    // Retorna direto os DTOs de leitura, sem passar pelo domínio
    List<OrderReportDTO> findReportByCriteria(GenerateOrderReportQuery query);

}
