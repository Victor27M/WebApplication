package com.victor.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// ── Recent order row for the dashboard table ──────────────────────────────────
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentOrderDTO {
    private UUID   id;
    private String personName;
    private String status;
    private String paymentStatus;
    private double amount;
    private String orderDate;
}