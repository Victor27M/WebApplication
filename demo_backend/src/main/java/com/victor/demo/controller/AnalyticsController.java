package com.victor.demo.controller;

import com.victor.demo.dto.*;
import com.victor.demo.service.AnalyticsService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/kpi")
    public ResponseEntity<KpiDTO> getKpi() {
        return ResponseEntity.ok(analyticsService.getKpi());
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenuePointDTO>> getRevenue(
            @RequestParam(defaultValue = "weekly") String period) {
        return ResponseEntity.ok(analyticsService.getRevenue(period));
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

    /**
     * GET /analytics/map-data
     * Returns one point per unique destination, with lat/lng from OpenRouteService
     * and the count of orders broken down by status. Used by the dashboard map.
     */
    @GetMapping("/map-data")
    public ResponseEntity<List<MapPointDTO>> getMapData() {
        return ResponseEntity.ok(analyticsService.getMapData());
    }
}
