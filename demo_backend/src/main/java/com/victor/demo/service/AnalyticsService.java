package com.victor.demo.service;

import com.victor.demo.dto.*;
import com.victor.demo.model.PaymentStatus;
import com.victor.demo.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final GeocodingService geocodingService;

    // ── KPI ────────────────────────────────────────────────────────────────────

    public KpiDTO getKpi() {
        double totalRevenue   = calcTotalRevenue();
        long   totalOrders    = orderRepository.count();
        long   paidOrders     = orderRepository.countByPaymentStatus(PaymentStatus.PAID);
        double avgOrderValue  = paidOrders > 0 ? totalRevenue / paidOrders : 0.0;
        long   totalCustomers = orderRepository.countDistinctCustomers();

        return new KpiDTO(
                Math.round(totalRevenue * 100.0) / 100.0,
                totalOrders,
                Math.round(avgOrderValue * 100.0) / 100.0,
                totalCustomers
        );
    }

    private double calcTotalRevenue() {
        Double result = orderRepository.sumRevenueForPaidOrders();
        return result != null ? result : 0.0;
    }

    // ── Revenue over time ─────────────────────────────────────────────────────

    public List<RevenuePointDTO> getRevenue(String period) {
        List<Object[]> rows = switch (period) {
            case "daily"   -> orderRepository.revenueByDay();
            case "monthly" -> orderRepository.revenueByMonth();
            default        -> orderRepository.revenueByWeek();
        };

        return rows.stream()
                .map(row -> new RevenuePointDTO(
                        row[0].toString(),
                        row[1] != null ? ((Number) row[1]).doubleValue() : 0.0
                ))
                .collect(Collectors.toList());
    }

    // ── Recent orders ─────────────────────────────────────────────────────────

    public List<RecentOrderDTO> getRecentOrders() {
        return orderRepository.findTop10ByOrderByOrderDateDesc()
                .stream()
                .map(o -> {
                    double amount = o.getItems().stream()
                            .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                            .sum();
                    return new RecentOrderDTO(
                            o.getId(),
                            o.getPerson().getName(),
                            o.getStatus().name(),
                            o.getPaymentStatus().name(),
                            Math.round(amount * 100.0) / 100.0,
                            o.getOrderDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    );
                })
                .collect(Collectors.toList());
    }

    // ── Order status breakdown ────────────────────────────────────────────────

    public Map<String, Long> getStatusBreakdown() {
        List<Object[]> rows = orderRepository.countByStatusGrouped();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return result;
    }

    // ── Top products ──────────────────────────────────────────────────────────

    public List<TopProductDTO> getTopProducts() {
        return orderRepository.topProducts().stream()
                .map(row -> new TopProductDTO(
                        row[0].toString(),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).doubleValue()
                ))
                .collect(Collectors.toList());
    }

    // ── Map data ──────────────────────────────────────────────────────────────

    /**
     * Groups orders by destination, geocodes each unique destination using
     * OpenRouteService, and returns lat/lng points for the dashboard map.
     * Destinations that fail to geocode are silently skipped.
     */
    public List<MapPointDTO> getMapData() {
        // Group orders by destination; count totals + breakdown per status
        List<Object[]> rows = orderRepository.countOrdersByDestinationAndStatus();

        // Map<destination, MapPointDto>
        Map<String, MapPointDTO> grouped = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String destination = row[0] != null ? row[0].toString() : null;
            String status      = row[1] != null ? row[1].toString() : "UNKNOWN";
            long   count       = ((Number) row[2]).longValue();

            if (destination == null || destination.isBlank()) continue;

            MapPointDTO point = grouped.computeIfAbsent(destination, d -> {
                MapPointDTO p = new MapPointDTO();
                p.setDestination(d);
                p.setStatuses(new LinkedHashMap<>());
                p.setCount(0);
                return p;
            });

            point.setCount(point.getCount() + count);
            point.getStatuses().merge(status, count, Long::sum);
        }

        // Geocode each unique destination
        return grouped.values().stream()
                .map(point -> {
                    Optional<GeocodingService.Coords> coords =
                            geocodingService.geocode(point.getDestination());
                    if (coords.isEmpty()) return null;
                    point.setLat(coords.get().getLat());
                    point.setLng(coords.get().getLng());
                    return point;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}