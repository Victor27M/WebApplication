package com.victor.demo.service;

import com.victor.demo.dto.RecentOrderDTO;
import com.victor.demo.model.Order;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Publishes order lifecycle events to the WebSocket topic /topic/orders.
 * The Angular dashboard subscribes to this and updates the live feed
 * without any page refresh needed.
 *
 * Call publish(order) after every orderRepository.save() in OrderService
 * and PaymentService.
 */
@Service
@AllArgsConstructor
public class OrderEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(Order order) {
        double amount = order.getItems().stream()
                .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                .sum();

        RecentOrderDTO dto = new RecentOrderDTO(
                order.getId(),
                order.getPerson().getName(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                Math.round(amount * 100.0) / 100.0,
                order.getOrderDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        messagingTemplate.convertAndSend("/topic/orders", dto);
    }
}