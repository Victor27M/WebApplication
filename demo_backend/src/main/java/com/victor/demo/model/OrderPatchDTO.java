package com.victor.demo.model;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class OrderPatchDTO {

    @Valid
    private List<OrderItemDTO> items;

    private String destination;

    private OrderStatus status;
}