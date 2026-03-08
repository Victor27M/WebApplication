package com.victor.demo.repository;

import com.victor.demo.model.Order;
import com.victor.demo.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByPersonId(UUID personId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByPersonIdAndStatus(UUID personId, OrderStatus status);
}