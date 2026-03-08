package com.victor.demo.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class OrderCreateDTO {

    @NotNull(message = "Person id is required")
    private UUID personId;

    @NotNull(message = "At least one item is required")
    @Size(min = 1, message = "An order must contain at least one item")
    @Valid
    private List<OrderItemDTO> items;

    private String destination;

    private OrderStatus status = OrderStatus.PENDING;
}