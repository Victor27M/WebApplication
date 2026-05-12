package com.victor.demo.controller;

import com.victor.demo.model.Order;
import com.victor.demo.service.PaymentService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payment")
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /payment/{orderId}/pay
     * Triggers payment processing for the given order.
     * Returns 200 with the updated order, or 409 if the transition is illegal.
     */
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<?> pay(@PathVariable UUID orderId) {
        try {
            Order updated = paymentService.processPayment(orderId);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(409).body(error);
        }
    }

    /**
     * POST /payment/{orderId}/refund
     * Issues a refund for the given order.
     * Returns 200 with the updated order, or 409 if the transition is illegal.
     */
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<?> refund(@PathVariable UUID orderId) {
        try {
            Order updated = paymentService.refund(orderId);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(409).body(error);
        }
    }
}
