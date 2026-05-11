package com.victor.demo.service;

import com.victor.demo.model.Order;
import com.victor.demo.model.PaymentStatus;
import com.victor.demo.payment.*;
import com.victor.demo.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates payment state transitions using the State pattern.
 *
 * Design decision: PaymentService acts as the context holder. It resolves
 * the correct PaymentState based on the order's current paymentStatus and
 * delegates the operation to that state. This keeps each state's logic
 * isolated and makes adding new states trivial.
 */
@Service
@AllArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order processPayment(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order with id " + orderId + " not found"));

        PaymentState state = resolveState(order.getPaymentStatus());
        Order updated = state.processPayment(order);
        return orderRepository.save(updated);
    }

    @Transactional
    public Order refund(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Order with id " + orderId + " not found"));

        PaymentState state = resolveState(order.getPaymentStatus());
        Order updated = state.refund(order);
        return orderRepository.save(updated);
    }

    /**
     * Factory method — maps a PaymentStatus to its corresponding state object.
     * Adding a new status only requires a new state class and one line here.
     */
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