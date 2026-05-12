package com.victor.demo.service;

import com.victor.demo.model.Order;
import com.victor.demo.payment.PaymentState;
import com.victor.demo.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class PaymentService {

    private final OrderRepository     orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Transactional
    public Order processPayment(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order with id " + orderId + " not found"));

        Order updated = PaymentState.resolve(order.getPaymentStatus()).processPayment(order);
        Order saved   = orderRepository.save(updated);
        orderEventPublisher.publish(saved);
        return saved;
    }

    @Transactional
    public Order refund(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order with id " + orderId + " not found"));

        Order updated = PaymentState.resolve(order.getPaymentStatus()).refund(order);
        Order saved   = orderRepository.save(updated);
        orderEventPublisher.publish(saved);
        return saved;
    }
}