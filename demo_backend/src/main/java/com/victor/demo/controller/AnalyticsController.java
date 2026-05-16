package com.victor.demo.controller;

import com.victor.demo.dto.*;
import com.victor.demo.service.AnalyticsService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/kpi")
    public ResponseEntity<KpiDTO> getKpi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getKpi(from, to));
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenuePointDTO>> getRevenue(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getRevenue(period, from, to));
    }

    @GetMapping("/recent-orders")
    public ResponseEntity<List<RecentOrderDTO>> getRecentOrders() {
        return ResponseEntity.ok(analyticsService.getRecentOrders());
    }

    @GetMapping("/order-status-breakdown")
    public ResponseEntity<Map<String, Long>> getStatusBreakdown() {
        return ResponseEntity.ok(analyticsService.getStatusBreakdown());
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductDTO>> getTopProducts() {
        return ResponseEntity.ok(analyticsService.getTopProducts());
    }

    @GetMapping("/map-data")
    public ResponseEntity<List<MapPointDTO>> getMapData() {
        return ResponseEntity.ok(analyticsService.getMapData());
    }
}
