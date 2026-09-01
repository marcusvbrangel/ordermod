package com.market.order.internal.adapter.out.persistence.jdbc;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface SpringDataOrderRepository extends CrudRepository<OrderJdbcEntity, UUID> {
}
