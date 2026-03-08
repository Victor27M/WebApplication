package com.victor.demo.controller;

import com.victor.demo.config.ValidationException;
import com.victor.demo.model.Order;
import com.victor.demo.model.OrderCreateDTO;
import com.victor.demo.model.OrderPatchDTO;
import com.victor.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@CrossOrigin
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<Order> getOrders() {
        return orderService.getOrders();
    }

    @GetMapping("/{uuid}")
    public Order getOrderById(@PathVariable UUID uuid) {
        return orderService.getOrderById(uuid);
    }

    @GetMapping("/person/{personId}")
    public List<Order> getOrdersByPerson(@PathVariable UUID personId) {
        return orderService.getOrdersByPerson(personId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order addOrder(@Valid @RequestBody OrderCreateDTO dto) throws ValidationException {
        return orderService.addOrder(dto);
    }

    @PutMapping("/{uuid}")
    public Order updateOrder(@PathVariable UUID uuid,
                             @Valid @RequestBody OrderCreateDTO dto) throws ValidationException {
        return orderService.updateOrder(uuid, dto);
    }

    @PatchMapping("/{uuid}")
    public Order patchOrder(@PathVariable UUID uuid,
                            @RequestBody OrderPatchDTO patch) throws ValidationException {
        return orderService.patchOrder(uuid, patch);
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable UUID uuid) throws ValidationException {
        orderService.deleteOrder(uuid);
    }
}