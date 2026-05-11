package com.victor.demo.payment;

import com.victor.demo.model.Order;
import com.victor.demo.model.PaymentStatus;

/**
 * State pattern — defines the contract each payment state must fulfil.
 * Each concrete state decides which transitions are legal and throws
 * IllegalStateException for invalid operations.
 */
public interface PaymentState {

    /**
     * Simulate payment processing: UNPAID → PROCESSING → PAID | FAILED.
     * States that do not allow this operation throw IllegalStateException.
     */
    Order processPayment(Order order);

    /**
     * Issue a refund: PAID → REFUNDED.
     * States that do not allow this operation throw IllegalStateException.
     */
    Order refund(Order order);

    PaymentStatus getStatus();
}