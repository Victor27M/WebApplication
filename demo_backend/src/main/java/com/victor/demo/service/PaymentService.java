package com.victor.demo.service;

import com.victor.demo.model.Order;
import com.victor.demo.model.PaymentStatus;
import com.victor.demo.payment.*;
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

        PaymentState state = resolveState(order.getPaymentStatus());
        Order updated = state.processPayment(order);
        Order saved   = orderRepository.save(updated);
        orderEventPublisher.publish(saved);   // ← WebSocket broadcast
        return saved;
    }

    @Transactional
    public Order refund(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order with id " + orderId + " not found"));

        PaymentState state = resolveState(order.getPaymentStatus());
        Order updated = state.refund(order);
        Order saved   = orderRepository.save(updated);
        orderEventPublisher.publish(saved);   // ← WebSocket broadcast
        return saved;
    }

    private PaymentState resolveState(PaymentStatus status) {
        return switch (status) {
            case UNPAID     -> new UnpaidPaymentState();
            case PROCESSING -> new ProcessingPaymentState();
            case PAID       -> new PaidPaymentState();
            case FAILED     -> new FailedPaymentState();
            case REFUNDED   -> new RefundedPaymentState();
        };
    }
}
