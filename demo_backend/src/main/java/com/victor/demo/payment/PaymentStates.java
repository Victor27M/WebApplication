package com.victor.demo.payment;

import com.victor.demo.model.Order;
import com.victor.demo.model.PaymentStatus;

// ─────────────────────────────────────────────────────────────────────────────
// UNPAID state — initial state, payment not yet attempted
// ─────────────────────────────────────────────────────────────────────────────
class UnpaidPaymentState implements PaymentState {

    @Override
    public Order processPayment(Order order) {
        // Move to PROCESSING first, then simulate async resolution → PAID
        order.setPaymentStatus(PaymentStatus.PAID);
        return order;
    }

    @Override
    public Order refund(Order order) {
        throw new IllegalStateException("Cannot refund an order that has not been paid yet.");
    }

    @Override
    public PaymentStatus getStatus() {
        return PaymentStatus.UNPAID;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROCESSING state — payment gateway contacted, awaiting response
// ─────────────────────────────────────────────────────────────────────────────
class ProcessingPaymentState implements PaymentState {

    @Override
    public Order processPayment(Order order) {
        throw new IllegalStateException("Payment is already being processed.");
    }

    @Override
    public Order refund(Order order) {
        throw new IllegalStateException("Cannot refund an order that is still processing.");
    }

    @Override
    public PaymentStatus getStatus() {
        return PaymentStatus.PROCESSING;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAID state — payment successful; refund is the only valid next step
// ─────────────────────────────────────────────────────────────────────────────
class PaidPaymentState implements PaymentState {

    @Override
    public Order processPayment(Order order) {
        throw new IllegalStateException("Order has already been paid.");
    }

    @Override
    public Order refund(Order order) {
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        return order;
    }

    @Override
    public PaymentStatus getStatus() {
        return PaymentStatus.PAID;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAILED state — payment attempt failed; customer can retry
// ─────────────────────────────────────────────────────────────────────────────
class FailedPaymentState implements PaymentState {

    @Override
    public Order processPayment(Order order) {
        // Allow retry: back to UNPAID → processPayment
        order.setPaymentStatus(PaymentStatus.UNPAID);
        PaymentState retryState = new UnpaidPaymentState();
        return retryState.processPayment(order);
    }

    @Override
    public Order refund(Order order) {
        throw new IllegalStateException("Cannot refund a failed payment.");
    }

    @Override
    public PaymentStatus getStatus() {
        return PaymentStatus.FAILED;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REFUNDED state — terminal state; no further transitions allowed
// ─────────────────────────────────────────────────────────────────────────────
class RefundedPaymentState implements PaymentState {

    @Override
    public Order processPayment(Order order) {
        throw new IllegalStateException("Cannot process payment for a refunded order.");
    }

    @Override
    public Order refund(Order order) {
        throw new IllegalStateException("Order has already been refunded.");
    }

    @Override
    public PaymentStatus getStatus() {
        return PaymentStatus.REFUNDED;
    }
}
