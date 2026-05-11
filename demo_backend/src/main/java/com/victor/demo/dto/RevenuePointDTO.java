package com.victor.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// ── Single data point for the revenue chart ───────────────────────────────────
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenuePointDTO{
    private String date;     // formatted: "2025-05-07" / "2025-W18" / "2025-05"
    private double revenue;
}