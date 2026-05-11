package com.victor.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ── Top-selling product entry ──────────────────────────────────────────────────
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopProductDTO {
    private String name;
    private long   unitsSold;
    private double revenue;
}