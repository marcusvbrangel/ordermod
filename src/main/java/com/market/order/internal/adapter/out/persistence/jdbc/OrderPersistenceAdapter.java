package com.market.order.internal.adapter.out.persistence.jdbc;

import com.market.order.internal.application.port.in.GenerateOrderReportQuery;
import com.market.order.internal.application.port.in.OrderReportDTO;
import com.market.order.internal.application.port.out.OrderQueryRepository;
import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderPersistenceAdapter implements OrderRepository, OrderQueryRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderPersistenceAdapter.class);

    private final SpringDataOrderRepository repository;
    private final OrderPersistenceMapper mapper;
    private final JdbcClient jdbcClient;

    public OrderPersistenceAdapter(
            SpringDataOrderRepository repository,
            OrderPersistenceMapper mapper,
            JdbcClient jdbcClient
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Order save(Order order) {
        LOGGER.debug("Saving order {} with {} item(s)", order.id().value(), order.items().size());
        var savedEntity = repository.save(mapper.toEntity(order));

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return repository.findById(orderId.value())
                .map(mapper::toDomain);
    }

    // Implementação da Porta de Leitura (CQRS)
    @Override
    public List<OrderReportDTO> findReportByCriteria(GenerateOrderReportQuery query) {

        // Define a query SQL nativa otimizada para o PostgreSQL do seu projeto
        String sql = """
            SELECT 
                o.id AS "orderId", 
                o.customer_id AS "customerId", 
                o.status AS "status", 
                o.total_amount AS "totalValue", 
                o.created_at AS "createdAt", 
                COUNT(i.id) AS "totalItems"
            FROM orders.orders o
            LEFT JOIN orders.order_items i ON i.order_id = o.id
            WHERE o.created_at BETWEEN :start AND :end
              AND (:status IS NULL OR o.status = :status)
            GROUP BY o.id
        """;

        // Executa a query com parâmetros nomeados e mapeia automaticamente para o Record
        return jdbcClient.sql(sql)
                .param("start", query.startDate().atStartOfDay())
                .param("end", query.endDate().atTime(23, 59, 59))
                .param("status", query.status())
                .query(OrderReportDTO.class) // O Spring Boot mapeia via reflexão direto pro Record!
                .list();

    }
}
