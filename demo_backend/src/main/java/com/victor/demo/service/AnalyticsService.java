package com.victor.demo.service;

import com.victor.demo.dto.*;
import com.victor.demo.model.PaymentStatus;
import com.victor.demo.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AnalyticsService {

    private final OrderRepository  orderRepository;
    private final GeocodingService geocodingService;

    // ── KPI ────────────────────────────────────────────────────────────────────

    public KpiDTO getKpi(LocalDate from, LocalDate to) {
        LocalDateTime start = resolveFrom(from).atStartOfDay();
        LocalDateTime end   = resolveTo(to).plusDays(1).atStartOfDay();

        Double revenue      = orderRepository.sumRevenueBetween(start, end);
        double totalRevenue = revenue != null ? revenue : 0.0;
        long   totalOrders  = orderRepository.countByOrderDateBetween(start, end);
        long   paidOrders   = orderRepository.countPaidBetween(start, end);
        double avgOrder     = paidOrders > 0 ? totalRevenue / paidOrders : 0.0;
        long   customers    = orderRepository.countDistinctCustomersBetween(start, end);

        return new KpiDTO(
                Math.round(totalRevenue * 100.0) / 100.0,
                totalOrders,
                Math.round(avgOrder * 100.0) / 100.0,
                customers
        );
    }

    // ── Revenue over time ─────────────────────────────────────────────────────

    public List<RevenuePointDTO> getRevenue(String period, LocalDate from, LocalDate to) {
        LocalDateTime start = resolveFrom(from).atStartOfDay();
        LocalDateTime end   = resolveTo(to).plusDays(1).atStartOfDay();

        List<Object[]> rows = switch (period) {
            case "daily"   -> orderRepository.revenueByDayBetween(start, end);
            case "monthly" -> orderRepository.revenueByMonthBetween(start, end);
            default        -> orderRepository.revenueByWeekBetween(start, end);
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

    public List<MapPointDTO> getMapData() {
        List<Object[]> rows = orderRepository.countOrdersByDestinationAndStatus();

        Map<String, MapPointDTO> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String destination = row[0] != null ? row[0].toString() : null;
            String status      = row[1] != null ? row[1].toString() : "UNKNOWN";
            long   count       = ((Number) row[2]).longValue();

            if (destination == null || destination.isBlank()) continue;

            map.computeIfAbsent(destination, d -> {
                var coords = geocodingService.geocode(d);
                if (coords.isEmpty()) return null;
                var c = coords.get();
                return new MapPointDTO(c.getLat(), c.getLng(), d, 0, new LinkedHashMap<>());
            });

            MapPointDTO point = map.get(destination);
            if (point == null) continue;

            point.setCount(point.getCount() + count);
            point.getStatuses().merge(status, count, Long::sum);
        }

        return map.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private LocalDate resolveFrom(LocalDate from) {
        return from != null ? from : LocalDate.now().minusMonths(12);
    }

    private LocalDate resolveTo(LocalDate to) {
        return to != null ? to : LocalDate.now();
    }
}
