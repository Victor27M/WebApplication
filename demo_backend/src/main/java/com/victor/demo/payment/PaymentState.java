package com.victor.demo.payment;

import com.victor.demo.model.Order;
import com.victor.demo.model.PaymentStatus;

/**
 * State pattern — defines the contract each payment state must fulfil.
 *
 * The static resolve() factory method is the only public entry point.
 * PaymentService calls PaymentState.resolve(status) instead of directly
 * instantiating state classes, so the concrete classes stay package-private.
 */
public interface PaymentState {

    Order processPayment(Order order);

    Order refund(Order order);

    PaymentStatus getStatus();

    /**
     * Factory — maps a PaymentStatus to the correct state object.
     * Lives here so PaymentService never needs to import the concrete classes.
     */
    static PaymentState resolve(PaymentStatus status) {
        return switch (status) {
            case UNPAID     -> new UnpaidPaymentState();
            case PROCESSING -> new ProcessingPaymentState();
            case PAID       -> new PaidPaymentState();
            case FAILED     -> new FailedPaymentState();
            case REFUNDED   -> new RefundedPaymentState();
        };
    }
}