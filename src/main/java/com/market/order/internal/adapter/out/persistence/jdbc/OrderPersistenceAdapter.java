package com.market.order.internal.adapter.out.persistence.jdbc;

import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OrderPersistenceAdapter implements OrderRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderPersistenceAdapter.class);

    private final SpringDataOrderRepository repository;
    private final OrderPersistenceMapper mapper;

    public OrderPersistenceAdapter(
            SpringDataOrderRepository repository,
            OrderPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
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
}
