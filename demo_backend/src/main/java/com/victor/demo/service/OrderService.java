package com.victor.demo.service;

import com.victor.demo.config.ValidationException;
import com.victor.demo.model.*;
import com.victor.demo.repository.OrderRepository;
import com.victor.demo.repository.PersonRepository;
import com.victor.demo.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PersonRepository personRepository;
    private final ProductRepository productRepository;

    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(UUID uuid) {
        return orderRepository.findById(uuid).orElseThrow(
                () -> new IllegalStateException("Order with id " + uuid + " not found"));
    }

    public List<Order> getOrdersByPerson(UUID personId) {
        return orderRepository.findByPersonId(personId);
    }

    @Transactional
    public Order addOrder(OrderCreateDTO dto) throws ValidationException {
        Person person = personRepository.findById(dto.getPersonId())
                .orElseThrow(() -> new ValidationException(
                        "Person with id " + dto.getPersonId() + " not found"));

        Order order = new Order();
        order.setPerson(person);
        order.setDestination(dto.getDestination());
        order.setStatus(dto.getStatus() != null ? dto.getStatus() : OrderStatus.PENDING);

        List<OrderItem> items = buildItems(dto.getItems(), order);
        order.setItems(items);

        // deduct stock if order is created already as SHIPPED or DELIVERED
        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED) {
            deductStock(items);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrder(UUID uuid, OrderCreateDTO dto) throws ValidationException {
        Order existing = orderRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException(
                        "Order with id " + uuid + " not found"));

        if (existing.getStatus() == OrderStatus.DELIVERED ||
                existing.getStatus() == OrderStatus.CANCELLED) {
            throw new ValidationException(
                    "Cannot update an order that is already " + existing.getStatus());
        }

        Person person = personRepository.findById(dto.getPersonId())
                .orElseThrow(() -> new ValidationException(
                        "Person with id " + dto.getPersonId() + " not found"));

        OrderStatus oldStatus = existing.getStatus();
        OrderStatus newStatus = dto.getStatus() != null ? dto.getStatus() : oldStatus;

        // restore old stock before applying new items
        if (oldStatus == OrderStatus.SHIPPED || oldStatus == OrderStatus.DELIVERED) {
            restoreStock(existing.getItems());
        }

        existing.getItems().clear();
        List<OrderItem> newItems = buildItems(dto.getItems(), existing);
        existing.getItems().addAll(newItems);

        existing.setPerson(person);
        existing.setDestination(dto.getDestination());
        existing.setStatus(newStatus);

        // deduct stock for new status
        if (newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.DELIVERED) {
            deductStock(newItems);
        }

        return orderRepository.save(existing);
    }

    @Transactional
    public Order patchOrder(UUID uuid, OrderPatchDTO patch) throws ValidationException {
        Order existing = orderRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException(
                        "Order with id " + uuid + " not found"));

        if (existing.getStatus() == OrderStatus.DELIVERED ||
                existing.getStatus() == OrderStatus.CANCELLED) {
            throw new ValidationException(
                    "Cannot patch an order that is already " + existing.getStatus());
        }

        OrderStatus oldStatus = existing.getStatus();

        if (patch.getItems() != null) {
            if (oldStatus == OrderStatus.SHIPPED) {
                restoreStock(existing.getItems());
            }
            existing.getItems().clear();
            List<OrderItem> newItems = buildItems(patch.getItems(), existing);
            existing.getItems().addAll(newItems);
        }

        if (patch.getDestination() != null) {
            existing.setDestination(patch.getDestination());
        }

        if (patch.getStatus() != null) {
            OrderStatus newStatus = patch.getStatus();

            boolean wasNotShipped = oldStatus != OrderStatus.SHIPPED &&
                    oldStatus != OrderStatus.DELIVERED;
            boolean isNowShipped  = newStatus == OrderStatus.SHIPPED ||
                    newStatus == OrderStatus.DELIVERED;

            if (wasNotShipped && isNowShipped) {
                deductStock(existing.getItems());
            }

            if ((oldStatus == OrderStatus.SHIPPED) &&
                    newStatus == OrderStatus.CANCELLED) {
                restoreStock(existing.getItems());
            }

            existing.setStatus(newStatus);
        }

        return orderRepository.save(existing);
    }

    public void deleteOrder(UUID uuid) throws ValidationException {
        Order order = orderRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException(
                        "Order with id " + uuid + " not found"));

        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED) {
            throw new ValidationException(
                    "Cannot delete an order that is already " + order.getStatus());
        }

        orderRepository.deleteById(uuid);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private List<OrderItem> buildItems(
            List<OrderItemDTO> dtoItems, Order order) throws ValidationException {

        if (dtoItems == null || dtoItems.isEmpty()) {
            throw new ValidationException("An order must contain at least one item");
        }

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemDTO dto : dtoItems) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new ValidationException(
                            "Product with id " + dto.getProductId() + " not found"));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(dto.getQuantity());
            items.add(item);
        }
        return items;
    }

    private void deductStock(List<OrderItem> items) throws ValidationException {
        for (OrderItem item : items) {
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity();
            if (newStock < 0) {
                throw new ValidationException(
                        "Insufficient stock for product: " + product.getName() +
                                ". Available: " + product.getStock() +
                                ", Requested: " + item.getQuantity());
            }
            product.setStock(newStock);
            productRepository.save(product);
        }
    }

    private void restoreStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }
}