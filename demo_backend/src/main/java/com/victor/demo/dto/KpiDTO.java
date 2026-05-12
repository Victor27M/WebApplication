package com.victor.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ── KPI summary ───────────────────────────────────────────────────────────────
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KpiDTO {
    private double totalRevenue;
    private long   totalOrders;
    private double avgOrderValue;
    private long   totalCustomers;
}
