package com.victor.demo.repository;

import com.victor.demo.model.Order;
import com.victor.demo.model.OrderStatus;
import com.victor.demo.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // ── Existing queries ───────────────────────────────────────────────────────
    List<Order> findByPersonId(UUID personId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByPersonIdAndStatus(UUID personId, OrderStatus status);

    // ── Payment ────────────────────────────────────────────────────────────────
    long countByPaymentStatus(PaymentStatus paymentStatus);

    // ── Analytics ─────────────────────────────────────────────────────────────

    @Query("SELECT SUM(oi.quantity * oi.product.price) FROM Order o JOIN o.items oi WHERE o.paymentStatus = 'PAID'")
    Double sumRevenueForPaidOrders();

    @Query("SELECT COUNT(DISTINCT o.person.id) FROM Order o")
    long countDistinctCustomers();

    // Revenue by day (last 90 days)
    @Query(value = """
        SELECT CAST(o.order_date AS date) AS day,
               SUM(oi.quantity * p.price)
        FROM orders o
        JOIN order_item oi ON oi.order_id = o.id
        JOIN product    p  ON p.id = oi.product_id
        WHERE o.payment_status = 'PAID'
          AND o.order_date >= NOW() - INTERVAL '90 days'
        GROUP BY day
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> revenueByDay();

    // Revenue by ISO week (last 52 weeks)
    @Query(value = """
        SELECT TO_CHAR(DATE_TRUNC('week', o.order_date), 'IYYY-"W"IW') AS week,
               SUM(oi.quantity * p.price)
        FROM orders o
        JOIN order_item oi ON oi.order_id = o.id
        JOIN product    p  ON p.id = oi.product_id
        WHERE o.payment_status = 'PAID'
          AND o.order_date >= NOW() - INTERVAL '52 weeks'
        GROUP BY DATE_TRUNC('week', o.order_date)
        ORDER BY DATE_TRUNC('week', o.order_date)
        """, nativeQuery = true)
    List<Object[]> revenueByWeek();

    // Revenue by month (last 12 months)
    @Query(value = """
        SELECT TO_CHAR(DATE_TRUNC('month', o.order_date), 'YYYY-MM') AS month,
               SUM(oi.quantity * p.price)
        FROM orders o
        JOIN order_item oi ON oi.order_id = o.id
        JOIN product    p  ON p.id = oi.product_id
        WHERE o.payment_status = 'PAID'
          AND o.order_date >= NOW() - INTERVAL '12 months'
        GROUP BY DATE_TRUNC('month', o.order_date)
        ORDER BY DATE_TRUNC('month', o.order_date)
        """, nativeQuery = true)
    List<Object[]> revenueByMonth();

    // Status breakdown
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatusGrouped();

    // Top 5 products by units sold and revenue
    @Query(value = """
        SELECT p.name,
               SUM(oi.quantity)           AS units_sold,
               SUM(oi.quantity * p.price) AS revenue
        FROM order_item oi
        JOIN product p ON p.id = oi.product_id
        JOIN orders  o ON o.id = oi.order_id
        WHERE o.payment_status = 'PAID'
        GROUP BY p.name
        ORDER BY units_sold DESC
        LIMIT 5
        """, nativeQuery = true)
    List<Object[]> topProducts();

    // Recent 10 orders for dashboard feed
    List<Order> findTop10ByOrderByOrderDateDesc();

    List<Object[]> countOrdersByDestinationAndStatus();
}